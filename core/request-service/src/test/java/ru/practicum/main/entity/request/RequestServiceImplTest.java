package ru.practicum.main.entity.request;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.main.client.EventClient;
import ru.practicum.main.client.UserClient;
import ru.practicum.main.dto.event.EventInternalDto;
import ru.practicum.main.dto.request.ParticipationRequestDto;
import ru.practicum.main.dto.user.UserInternalDto;
import ru.practicum.main.entity.event.EventState;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestServiceImplTest {

    @Mock
    private RequestRepository requestRepository;
    @Mock
    private EventClient eventClient;
    @Mock
    private UserClient userClient;

    @InjectMocks
    private RequestServiceImpl requestService;

    @Test
    void saveRequest_shouldSaveRequest() {
        EventInternalDto event = event(EventState.PUBLISHED, 99L, 0);
        Request savedRequest = Request.builder()
                .id(10L)
                .requesterId(1L)
                .eventId(2L)
                .createdAt(LocalDateTime.now())
                .status(RequestStatus.PENDING)
                .build();
        when(requestRepository.existsByRequesterIdAndEventId(1L, 2L)).thenReturn(false);
        when(userClient.getUser(1L)).thenReturn(UserInternalDto.builder().id(1L).build());
        when(eventClient.getEvent(2L)).thenReturn(event);
        when(requestRepository.save(any(Request.class))).thenReturn(savedRequest);

        ParticipationRequestDto result = requestService.saveRequest(1L, 2L);

        assertEquals(10L, result.getId());
        verify(requestRepository).save(any(Request.class));
    }

    @Test
    void saveRequest_shouldThrowConflict_whenDuplicateRequest() {
        when(requestRepository.existsByRequesterIdAndEventId(1L, 2L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> requestService.saveRequest(1L, 2L));
        verifyNoInteractions(eventClient, userClient);
    }

    @Test
    void saveRequest_shouldThrowNotFound_whenEventNotExists() {
        when(requestRepository.existsByRequesterIdAndEventId(1L, 2L)).thenReturn(false);
        when(userClient.getUser(1L)).thenReturn(UserInternalDto.builder().id(1L).build());
        when(eventClient.getEvent(2L)).thenThrow(new NotFoundException("Event not found"));

        assertThrows(NotFoundException.class, () -> requestService.saveRequest(1L, 2L));
    }

    @Test
    void saveRequest_shouldThrowConflict_whenUserIsInitiator() {
        when(requestRepository.existsByRequesterIdAndEventId(1L, 2L)).thenReturn(false);
        when(userClient.getUser(1L)).thenReturn(UserInternalDto.builder().id(1L).build());
        when(eventClient.getEvent(2L)).thenReturn(event(EventState.PUBLISHED, 1L, 0));

        assertThrows(ConflictException.class, () -> requestService.saveRequest(1L, 2L));
    }

    @Test
    void saveRequest_shouldThrowConflict_whenEventNotPublished() {
        when(requestRepository.existsByRequesterIdAndEventId(1L, 2L)).thenReturn(false);
        when(userClient.getUser(1L)).thenReturn(UserInternalDto.builder().id(1L).build());
        when(eventClient.getEvent(2L)).thenReturn(event(EventState.CANCELED, 99L, 0));

        assertThrows(ConflictException.class, () -> requestService.saveRequest(1L, 2L));
    }

    @Test
    void saveRequest_shouldThrowConflict_whenParticipantLimitReached() {
        when(requestRepository.existsByRequesterIdAndEventId(1L, 2L)).thenReturn(false);
        when(userClient.getUser(1L)).thenReturn(UserInternalDto.builder().id(1L).build());
        when(eventClient.getEvent(2L)).thenReturn(event(EventState.PUBLISHED, 99L, 5));
        when(requestRepository.countByEventIdAndStatus(2L, RequestStatus.CONFIRMED)).thenReturn(5L);

        assertThrows(ConflictException.class, () -> requestService.saveRequest(1L, 2L));
    }

    @Test
    void cancelRequest_shouldCancelRequest() {
        Request request = Request.builder()
                .id(10L)
                .requesterId(1L)
                .eventId(2L)
                .status(RequestStatus.CONFIRMED)
                .createdAt(LocalDateTime.now())
                .build();
        when(requestRepository.findById(10L)).thenReturn(Optional.of(request));

        ParticipationRequestDto result = requestService.cancelRequest(1L, 10L);

        assertEquals("CANCELED", result.getStatus());
        assertEquals(RequestStatus.CANCELED, request.getStatus());
    }

    @Test
    void cancelRequest_shouldThrowConflict_whenRequestBelongsToAnotherUser() {
        Request request = Request.builder().id(10L).requesterId(2L).build();
        when(requestRepository.findById(10L)).thenReturn(Optional.of(request));

        assertThrows(ConflictException.class, () -> requestService.cancelRequest(1L, 10L));
    }

    @Test
    void getUserRequests_shouldReturnRequests() {
        Request request = Request.builder()
                .id(1L)
                .requesterId(1L)
                .eventId(2L)
                .createdAt(LocalDateTime.now())
                .status(RequestStatus.CONFIRMED)
                .build();
        when(userClient.getUser(1L)).thenReturn(UserInternalDto.builder().id(1L).build());
        when(requestRepository.findByRequesterId(1L)).thenReturn(List.of(request));

        List<ParticipationRequestDto> result = requestService.getUserRequests(1L);

        assertEquals(1, result.size());
        verify(userClient).getUser(1L);
    }

    private EventInternalDto event(EventState state, Long initiatorId, Integer participantLimit) {
        return EventInternalDto.builder()
                .id(2L)
                .initiatorId(initiatorId)
                .state(state)
                .participantLimit(participantLimit)
                .requestModeration(true)
                .build();
    }
}
