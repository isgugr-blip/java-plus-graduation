package ru.practicum.main.entity.event;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.client.UserClient;
import ru.practicum.main.dto.category.CategoryDto;
import ru.practicum.main.dto.event.EventAdminParamDto;
import ru.practicum.main.dto.event.EventFullDto;
import ru.practicum.main.dto.event.EventInternalDto;
import ru.practicum.main.dto.event.EventParamDto;
import ru.practicum.main.dto.event.EventPublicParamDto;
import ru.practicum.main.dto.event.EventRequestsCountDto;
import ru.practicum.main.dto.event.EventShortDto;
import ru.practicum.main.dto.event.NewEventDto;
import ru.practicum.main.dto.event.UpdateEventAdminRequest;
import ru.practicum.main.dto.event.UpdateEventUserRequest;
import ru.practicum.main.dto.user.UserInternalDto;
import ru.practicum.main.entity.category.Category;
import ru.practicum.main.entity.category.CategoryMapper;
import ru.practicum.main.entity.category.CategoryService;
import ru.practicum.main.entity.request.RequestStatus;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.main.exception.ValidationException;
import ru.practicum.stat.client.StatClient;
import ru.practicum.stat.client.StatsRequest;
import ru.practicum.stat.dto.EndpointHit;
import ru.practicum.stat.dto.ViewStats;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventServiceImpl implements EventService {

    private final StatClient statClient;
    private final EventRepository eventRepository;
    private final CategoryService categoryService;
    private final RequestClientAdapter requestClient;
    private final UserClient userClient;

    private static final String APP_NAME = "ewm-main-service";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional
    public EventFullDto create(Long userId, NewEventDto dto) {
        UserInternalDto user = userClient.getUser(userId);
        CategoryDto categoryDto = categoryService.findById(dto.getCategory());
        Category category = CategoryMapper.toEntity(categoryDto);

        Event event = EventMapper.mapToEntity(dto, category, user);
        event = eventRepository.save(event);

        var result = EventMapper.toEventFullDto(event, 0L, 0L);
        log.info("Новое событие: {}", EventMapper.toLogEvent(result));
        return result;
    }

    @Override
    public EventFullDto getById(Long userId, Long eventId) {
        userClient.getUser(userId);
        Event event = getEvent(eventId);
        Long views = getViewsMap(Collections.singletonList(event)).getOrDefault(event.getId(), 0L);
        long confirmedRequests = getEventRequestsCount(eventId);
        return EventMapper.toEventFullDto(event, confirmedRequests, views);
    }

    @Override
    public List<Event> getByIds(Collection<Long> ids) {
        return eventRepository.findAllById(ids);
    }

    @Override
    public List<EventShortDto> getUserEvents(Long userId, Pageable pageable) {
        userClient.getUser(userId);
        List<Event> events = eventRepository.findAllByInitiatorId(userId, pageable);
        if (events.isEmpty()) {
            return Collections.emptyList();
        }
        return EventMapper.toEventShortDto(events, getEventsRequests(events), getViewsMap(events));
    }

    @Override
    @Transactional
    public EventFullDto updateEventByCreatorId(EventParamDto eventParamDto, UpdateEventUserRequest request) {
        Long userId = eventParamDto.getUserId();
        userClient.getUser(userId);
        Event event = getEvent(eventParamDto.getEventId());
        checkUserIsEventInitiator(userId, event);
        checkEventUpdateConditions(event, request);
        updateEventFromUserRequest(request, event);

        long requestsCount = getEventRequestsCount(event.getId());
        Long views = getViewsMap(Collections.singletonList(event)).getOrDefault(event.getId(), 0L);
        EventFullDto result = EventMapper.toEventFullDto(event, requestsCount, views);
        log.info("Событие обновлено создателем: {}", EventMapper.toLogEvent(result));
        return result;
    }

    @Override
    public List<EventFullDto> getEventsByAdmin(EventAdminParamDto params) {
        Pageable pageable = PageRequest.of(params.getFrom() / params.getSize(), params.getSize());
        Predicate predicate = buildAdminPredicate(params);
        List<Event> events = predicate == null
                ? eventRepository.findAll(pageable).getContent()
                : eventRepository.findAll(predicate, pageable).getContent();
        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Long> viewsMap = getViewsMap(events);
        Map<Long, Long> requestsMap = getEventsRequests(events);
        return events.stream()
                .map(event -> EventMapper.toEventFullDto(event,
                        requestsMap.getOrDefault(event.getId(), 0L),
                        viewsMap.getOrDefault(event.getId(), 0L)))
                .toList();
    }

    @Override
    @Transactional
    public EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest request) {
        Event event = getEvent(eventId);
        updateEventFromAdminRequest(request, event);
        applyStateAction(event, request.getStateAction());

        Long views = getViewsMap(Collections.singletonList(event)).getOrDefault(event.getId(), 0L);
        var result = EventMapper.toEventFullDto(event, getEventRequestsCount(eventId), views);
        log.info("Событие обновлено админом: {}", EventMapper.toLogEvent(result));
        return result;
    }

    @Override
    public List<EventShortDto> getPublishedEvents(EventPublicParamDto params, HttpServletRequest request) {
        validateDateRange(params.getRangeStart(), params.getRangeEnd());
        LocalDateTime rangeStart = params.getRangeStart() != null ? params.getRangeStart() : LocalDateTime.now();
        Pageable pageable = buildPageable(params.getSort(), params.getFrom(), params.getSize());
        Predicate predicate = buildPublicPredicate(params, rangeStart);
        List<Event> events = predicate == null
                ? eventRepository.findAll(pageable).getContent()
                : eventRepository.findAll(predicate, pageable).getContent();

        sendHit(request);
        if (events.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Long> viewsMap = getViewsMap(events);
        Map<Long, Long> requestsMap = getEventsRequests(events);
        List<EventShortDto> result = new ArrayList<>();
        for (Event event : events) {
            Long views = viewsMap.getOrDefault(event.getId(), 0L);
            Long requestsCount = requestsMap.getOrDefault(event.getId(), 0L);
            if (params.isOnlyAvailable()) {
                long remainedRequests = event.getParticipantLimit() == 0
                        ? 1 : event.getParticipantLimit() - requestsCount;
                if (remainedRequests > 0) {
                    result.add(EventMapper.toEventShortDto(event, requestsCount, views));
                }
            } else {
                result.add(EventMapper.toEventShortDto(event, requestsCount, views));
            }
        }

        if (SortBy.VIEWS == params.getSort()) {
            result.sort(Comparator.comparing(EventShortDto::getViews).reversed());
        }
        return result;
    }

    @Override
    public EventFullDto getPublishedEventById(Long id, HttpServletRequest request) {
        Event event = findEventByIdAndState(id, EventState.PUBLISHED);
        sendHit(request);
        Long views = getViewsMap(Collections.singletonList(event)).getOrDefault(event.getId(), 0L);
        return EventMapper.toEventFullDto(event, getEventRequestsCount(id), views);
    }

    @Override
    public Map<Long, Long> getEventsRequests(List<Event> events) {
        if (events.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> eventIds = events.stream().map(Event::getId).collect(Collectors.toSet());
        List<EventRequestsCountDto> requests = requestClient.countByEventIdsAndStatus(eventIds, RequestStatus.CONFIRMED);
        return requests.stream().collect(Collectors.toMap(EventRequestsCountDto::eventId, EventRequestsCountDto::count));
    }

    @Override
    public Map<Long, Long> getViewsMap(List<Event> events) {
        if (events.isEmpty()) {
            return Collections.emptyMap();
        }
        LocalDateTime start = events.stream()
                .map(Event::getCreatedOn)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now().minusYears(1));
        List<String> uris = events.stream().map(event -> "/events/" + event.getId()).toList();
        List<ViewStats> stats = statClient.getViewStats(new StatsRequest(
                start.format(FORMATTER),
                LocalDateTime.now().plusYears(1).format(FORMATTER),
                uris, true));
        return stats.stream().collect(Collectors.toMap(
                vs -> Long.parseLong(vs.getUri().substring("/events/".length())), ViewStats::getHits));
    }

    @Override
    public Event findEventByIdAndState(Long id, EventState state) {
        return eventRepository.findByIdAndState(id, state)
                .orElseThrow(() -> new NotFoundException("Event with id=" + id + " was not found"));
    }

    @Override
    public EventInternalDto getInternalEvent(Long eventId) {
        Event event = getEvent(eventId);
        return EventInternalDto.builder()
                .id(event.getId())
                .initiatorId(event.getInitiatorId())
                .state(event.getState())
                .participantLimit(event.getParticipantLimit())
                .requestModeration(event.getRequestModeration())
                .build();
    }

    @Override
    public boolean isEventExists(Long eventId) {
        return eventRepository.existsById(eventId);
    }

    private Predicate buildAdminPredicate(EventAdminParamDto params) {
        QEvent event = QEvent.event;
        BooleanBuilder builder = new BooleanBuilder();
        if (params.getUsers() != null && !params.getUsers().isEmpty()) {
            builder.and(event.initiatorId.in(params.getUsers()));
        }
        if (params.getStates() != null && !params.getStates().isEmpty()) {
            builder.and(event.state.in(params.getStates().stream()
                    .map(stateStr -> EventState.valueOf(stateStr.toUpperCase())).toList()));
        }
        if (params.getCategories() != null && !params.getCategories().isEmpty()) {
            builder.and(event.category.id.in(params.getCategories()));
        }
        if (params.getRangeStart() != null) {
            builder.and(event.eventDate.goe(params.getRangeStart()));
        }
        if (params.getRangeEnd() != null) {
            builder.and(event.eventDate.loe(params.getRangeEnd()));
        }
        return builder.getValue();
    }

    private Predicate buildPublicPredicate(EventPublicParamDto params, LocalDateTime rangeStart) {
        QEvent event = QEvent.event;
        BooleanBuilder builder = new BooleanBuilder();
        builder.and(event.state.eq(EventState.PUBLISHED));
        if (params.getText() != null && !params.getText().isBlank()) {
            String text = params.getText();
            builder.and(new BooleanBuilder()
                    .or(event.annotation.containsIgnoreCase(text))
                    .or(event.description.containsIgnoreCase(text)));
        }
        if (params.getCategories() != null && !params.getCategories().isEmpty()) {
            builder.and(event.category.id.in(params.getCategories()));
        }
        if (params.getPaid() != null) {
            builder.and(event.paid.eq(params.getPaid()));
        }
        if (rangeStart != null) {
            builder.and(event.eventDate.goe(rangeStart));
        }
        if (params.getRangeEnd() != null) {
            builder.and(event.eventDate.loe(params.getRangeEnd()));
        }
        return builder.getValue();
    }

    private void applyStateAction(Event event, AdminStateAction stateAction) {
        if (stateAction == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        switch (stateAction) {
            case PUBLISH_EVENT -> {
                if (event.getState() != EventState.PENDING) {
                    throw new ConflictException("Cannot publish the event because it's not in the right state: " + event.getState());
                }
                if (event.getEventDate().isBefore(now.plusHours(1))) {
                    throw new ConflictException("Cannot publish the event because event date is less than 1 hour from now.");
                }
                event.setState(EventState.PUBLISHED);
                event.setPublishedOn(now);
            }
            case REJECT_EVENT -> {
                if (event.getState() == EventState.PUBLISHED) {
                    throw new ConflictException("Cannot reject the event because it's already published.");
                }
                event.setState(EventState.CANCELED);
            }
            default -> throw new ConflictException("Invalid state action: " + stateAction);
        }
    }

    private void validateDateRange(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw new ValidationException("Range end must be after range start");
        }
    }

    private Pageable buildPageable(SortBy sort, int from, int size) {
        return SortBy.EVENT_DATE == sort
                ? PageRequest.of(from / size, size, Sort.by("eventDate").ascending())
                : PageRequest.of(from / size, size);
    }

    private void sendHit(HttpServletRequest request) {
        EndpointHit endpointHit = EndpointHit.builder()
                .app(APP_NAME)
                .uri(request.getRequestURI())
                .ip(request.getRemoteAddr())
                .timestamp(LocalDateTime.now().format(FORMATTER))
                .build();
        statClient.hit(endpointHit);
        log.info("На сервер статистики отправлен endpointHit: {}", endpointHit);
    }

    private Event getEvent(Long eventId) {
        return eventRepository.findWithRelationsById(eventId).orElseThrow(
                () -> new NotFoundException("Event with id " + eventId + " not found"));
    }

    private void updateEventFromUserRequest(UpdateEventUserRequest request, Event event) {
        ofNullable(request.getAnnotation()).ifPresent(event::setAnnotation);
        ofNullable(request.getDescription()).ifPresent(event::setDescription);
        ofNullable(request.getEventDate()).ifPresent(event::setEventDate);
        ofNullable(request.getLocation()).ifPresent(event::setLocation);
        ofNullable(request.getPaid()).ifPresent(event::setPaid);
        ofNullable(request.getParticipantLimit()).ifPresent(event::setParticipantLimit);
        ofNullable(request.getRequestModeration()).ifPresent(event::setRequestModeration);
        ofNullable(request.getTitle()).ifPresent(event::setTitle);
        if (request.getCategory() != null) {
            CategoryDto categoryDto = categoryService.findById(request.getCategory());
            event.setCategory(CategoryMapper.toEntity(categoryDto));
        }
        if (request.getStateAction() == UserStateAction.CANCEL_REVIEW) {
            event.setState(EventState.CANCELED);
        }
        if (request.getStateAction() == UserStateAction.SEND_TO_REVIEW) {
            event.setState(EventState.PENDING);
        }
    }

    private void updateEventFromAdminRequest(UpdateEventAdminRequest request, Event event) {
        if (request.getEventDate() != null && request.getEventDate().isBefore(LocalDateTime.now())) {
            throw new ValidationException("Event date cannot be in the past");
        }
        if (request.getCategory() != null) {
            CategoryDto categoryDto = categoryService.findById(request.getCategory());
            event.setCategory(CategoryMapper.toEntity(categoryDto));
        }
        ofNullable(request.getAnnotation()).ifPresent(event::setAnnotation);
        ofNullable(request.getDescription()).ifPresent(event::setDescription);
        ofNullable(request.getEventDate()).ifPresent(event::setEventDate);
        ofNullable(request.getLocation()).ifPresent(event::setLocation);
        ofNullable(request.getPaid()).ifPresent(event::setPaid);
        ofNullable(request.getParticipantLimit()).ifPresent(event::setParticipantLimit);
        ofNullable(request.getRequestModeration()).ifPresent(event::setRequestModeration);
        ofNullable(request.getTitle()).ifPresent(event::setTitle);
    }

    private void checkUserIsEventInitiator(Long userId, Event event) {
        if (!event.getInitiatorId().equals(userId)) {
            throw new ConflictException(String.format(
                    "User with id %d didn't initiate event with id %d", userId, event.getId()));
        }
    }

    private void checkEventUpdateConditions(Event event, UpdateEventUserRequest request) {
        if (event.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("Only pending or canceled events can be changed");
        }
        if (request.getEventDate() != null && !request.getEventDate().isAfter(LocalDateTime.now().plusHours(2))) {
            throw new ValidationException("Event date must be at least two hours in the future.");
        }
    }

    private long getEventRequestsCount(Long eventId) {
        List<EventRequestsCountDto> result = requestClient.countByEventIdsAndStatus(
                Set.of(eventId), RequestStatus.CONFIRMED);
        return result.isEmpty() ? 0L : result.getFirst().count();
    }
}
