package ru.practicum.main.entity.request;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.client.EventClient;
import ru.practicum.main.client.UserClient;
import ru.practicum.main.dto.event.EventInternalDto;
import ru.practicum.main.dto.event.EventRequestStatusUpdateRequest;
import ru.practicum.main.dto.event.EventRequestStatusUpdateResult;
import ru.practicum.main.dto.event.EventRequestsCountDto;
import ru.practicum.main.dto.request.ParticipationRequestDto;
import ru.practicum.main.entity.event.EventState;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final EventClient eventClient;
    private final UserClient userClient;

    @Override
    @Transactional
    public ParticipationRequestDto saveRequest(Long userId, Long eventId) {
        if (requestRepository.existsByRequesterIdAndEventId(userId, eventId)) {
            throw new ConflictException("Attempt to send a duplicate request.");
        }
        userClient.getUser(userId);
        EventInternalDto event = eventClient.getEvent(eventId);
        checkUserRequestForEvent(event, userId);

        Request request = requestRepository.save(RequestMapper.toRequest(event, userId));
        var result = RequestMapper.toRequestDto(request);
        log.info("Новая заявка на событие: {}", result);
        return result;
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        Request request = getRequest(requestId);
        if (!request.getRequesterId().equals(userId)) {
            throw new ConflictException("You can only cancel your own participation requests.");
        }
        request.setStatus(RequestStatus.CANCELED);
        var result = RequestMapper.toRequestDto(request);
        log.info("Заявка на событие отменена: {}", result);
        return result;
    }

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        userClient.getUser(userId);
        return RequestMapper.toRequestDto(requestRepository.findByRequesterId(userId));
    }

    @Override
    public List<EventRequestsCountDto> countByEventIdsAndStatus(Set<Long> ids, RequestStatus status) {
        if (ids.isEmpty()) {
            return List.of();
        }
        return requestRepository.countByEventIdInAndStatus(ids, status);
    }

    @Override
    public List<ParticipationRequestDto> getEventRequestsByCreatorId(Long userId, Long eventId) {
        userClient.getUser(userId);
        checkEventInitiator(userId, eventClient.getEvent(eventId));
        return RequestMapper.toRequestDto(requestRepository.findByEventId(eventId));
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateEventRequestsStatus(Long userId, Long eventId,
                                                                     EventRequestStatusUpdateRequest request) {
        userClient.getUser(userId);
        EventInternalDto event = eventClient.getEvent(eventId);
        checkEventInitiator(userId, event);
        List<Request> requests = requestRepository.findByIdInAndEventId(request.getRequestIds(), eventId);

        long confirmedRequests = requestRepository.countByEventIdAndStatus(eventId, RequestStatus.CONFIRMED);
        long remainedRequests = event.getParticipantLimit() - confirmedRequests;
        RequestStatus targetStatus = request.getStatus();
        if (targetStatus == RequestStatus.CONFIRMED) {
            if (event.getParticipantLimit() == 0 || !event.getRequestModeration()) {
                throw new ConflictException("Confirmation is not required or allowed for this event configuration.");
            }
            if (remainedRequests <= 0L) {
                throw new ConflictException("The participant limit has been reached");
            }
        }

        List<Request> confirmed = new ArrayList<>();
        List<Request> rejected = new ArrayList<>();
        for (Request req : requests) {
            if (req.getStatus() != RequestStatus.PENDING) {
                throw new ConflictException("Request must have status PENDING. Request with id="
                        + req.getId() + " has status " + req.getStatus());
            }
            if (targetStatus == RequestStatus.REJECTED) {
                req.setStatus(RequestStatus.REJECTED);
                rejected.add(req);
            } else if (remainedRequests <= 0L) {
                req.setStatus(RequestStatus.REJECTED);
                rejected.add(req);
            } else {
                req.setStatus(RequestStatus.CONFIRMED);
                confirmed.add(req);
                remainedRequests--;
            }
        }

        var result = new EventRequestStatusUpdateResult(
                RequestMapper.toRequestDto(confirmed), RequestMapper.toRequestDto(rejected));
        log.info("Обновлены статусы заявок на событие: {}", result);
        return result;
    }

    private Request getRequest(Long requestId) {
        return requestRepository.findById(requestId).orElseThrow(
                () -> new NotFoundException("Request with id " + requestId + " not found."));
    }

    private void checkEventInitiator(Long userId, EventInternalDto event) {
        if (!event.getInitiatorId().equals(userId)) {
            throw new ConflictException(String.format(
                    "User with id %d didn't initiate event with id %d", userId, event.getId()));
        }
    }

    private void checkUserRequestForEvent(EventInternalDto event, Long userId) {
        if (event.getInitiatorId().equals(userId)) {
            throw new ConflictException("The initiator can't request to join their own event.");
        }
        if (!event.getState().equals(EventState.PUBLISHED)) {
            throw new ConflictException("The Participation in unpublished events is forbidden.");
        }
        if (event.getParticipantLimit() != 0) {
            long requests = requestRepository.countByEventIdAndStatus(event.getId(), RequestStatus.CONFIRMED);
            if (requests >= event.getParticipantLimit()) {
                throw new ConflictException("The participant limit has been reached.");
            }
        }
    }
}
