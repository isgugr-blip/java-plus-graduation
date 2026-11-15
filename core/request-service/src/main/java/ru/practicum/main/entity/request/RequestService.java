package ru.practicum.main.entity.request;

import ru.practicum.main.dto.event.EventRequestsCountDto;
import ru.practicum.main.dto.event.EventRequestStatusUpdateRequest;
import ru.practicum.main.dto.event.EventRequestStatusUpdateResult;
import ru.practicum.main.dto.request.ParticipationRequestDto;

import java.util.List;
import java.util.Set;

public interface RequestService {

    ParticipationRequestDto saveRequest(Long userId, Long eventId);

    ParticipationRequestDto cancelRequest(Long userId, Long requestId);

    List<ParticipationRequestDto> getUserRequests(Long userId);

    List<EventRequestsCountDto> countByEventIdsAndStatus(Set<Long> ids, RequestStatus status);

    List<ParticipationRequestDto> getEventRequestsByCreatorId(Long userId, Long eventId);

    EventRequestStatusUpdateResult updateEventRequestsStatus(Long userId, Long eventId,
                                                              EventRequestStatusUpdateRequest request);
}
