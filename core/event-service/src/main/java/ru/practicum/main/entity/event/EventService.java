package ru.practicum.main.entity.event;

import org.springframework.data.domain.Pageable;
import ru.practicum.main.dto.event.EventAdminParamDto;
import ru.practicum.main.dto.event.EventFullDto;
import ru.practicum.main.dto.event.EventInternalDto;
import ru.practicum.main.dto.event.EventParamDto;
import ru.practicum.main.dto.event.EventPublicParamDto;
import ru.practicum.main.dto.event.EventShortDto;
import ru.practicum.main.dto.event.NewEventDto;
import ru.practicum.main.dto.event.UpdateEventAdminRequest;
import ru.practicum.main.dto.event.UpdateEventUserRequest;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface EventService {
    List<EventFullDto> getEventsByAdmin(EventAdminParamDto params);

    EventFullDto updateEventByAdmin(Long eventId, UpdateEventAdminRequest request);

    List<EventShortDto> getPublishedEvents(EventPublicParamDto params);

    EventFullDto getPublishedEventById(Long id, long userId);

    List<EventShortDto> getRecommendations(long userId, int maxResults);

    void likeEvent(long userId, Long eventId);

    EventFullDto create(Long userId, NewEventDto dto);

    EventFullDto getById(Long userId, Long eventId);

    List<EventShortDto> getUserEvents(Long userId, Pageable pageable);

    List<Event> getByIds(Collection<Long> ids);

    EventFullDto updateEventByCreatorId(EventParamDto eventParamDto, UpdateEventUserRequest request);

    Map<Long, Long> getEventsRequests(List<Event> events);

    Map<Long, Double> getRatingsMap(List<Event> events);

    Event findEventByIdAndState(Long id, EventState state);

    EventInternalDto getInternalEvent(Long eventId);

    boolean isEventExists(Long eventId);
}
