package ru.practicum.main.entity.request;

import org.junit.jupiter.api.Test;
import ru.practicum.main.dto.event.EventInternalDto;
import ru.practicum.main.dto.request.ParticipationRequestDto;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequestMapperTest {

    @Test
    void toRequest_shouldCreatePendingRequest_whenModerationEnabled() {
        EventInternalDto event = EventInternalDto.builder()
                .id(1L)
                .participantLimit(5)
                .requestModeration(true)
                .build();

        Request request = RequestMapper.toRequest(event, 2L);

        assertNotNull(request);
        assertEquals(1L, request.getEventId());
        assertEquals(2L, request.getRequesterId());
        assertEquals(RequestStatus.PENDING, request.getStatus());
        assertNotNull(request.getCreatedAt());
    }

    @Test
    void toRequest_shouldCreateConfirmedRequest_whenModerationDisabled() {
        EventInternalDto event = EventInternalDto.builder()
                .id(1L)
                .participantLimit(0)
                .requestModeration(false)
                .build();

        Request request = RequestMapper.toRequest(event, 2L);

        assertEquals(RequestStatus.CONFIRMED, request.getStatus());
    }

    @Test
    void toRequestDto_shouldMapRequestToDto() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 12, 30, 45);
        Request request = Request.builder()
                .id(100L)
                .eventId(10L)
                .requesterId(20L)
                .createdAt(createdAt)
                .status(RequestStatus.CONFIRMED)
                .build();

        ParticipationRequestDto dto = RequestMapper.toRequestDto(request);

        assertEquals(100L, dto.getId());
        assertEquals("2026-01-01 12:30:45", dto.getCreated());
        assertEquals(10L, dto.getEventId());
        assertEquals(20L, dto.getRequesterId());
        assertEquals("CONFIRMED", dto.getStatus());
    }

    @Test
    void toRequestDtos_shouldMapList() {
        Request request1 = Request.builder()
                .id(10L)
                .eventId(1L)
                .requesterId(2L)
                .createdAt(LocalDateTime.now())
                .status(RequestStatus.PENDING)
                .build();
        Request request2 = Request.builder()
                .id(20L)
                .eventId(1L)
                .requesterId(2L)
                .createdAt(LocalDateTime.now())
                .status(RequestStatus.CONFIRMED)
                .build();

        List<ParticipationRequestDto> result = RequestMapper.toRequestDto(List.of(request1, request2));

        assertEquals(2, result.size());
        assertEquals(10L, result.get(0).getId());
        assertEquals(20L, result.get(1).getId());
    }

    @Test
    void toRequestDtos_shouldReturnEmptyList() {
        List<ParticipationRequestDto> result = RequestMapper.toRequestDto(Collections.emptyList());

        assertTrue(result.isEmpty());
    }
}
