package ru.practicum.main.entity.request;

import lombok.experimental.UtilityClass;
import ru.practicum.main.dto.event.EventInternalDto;
import ru.practicum.main.dto.request.ParticipationRequestDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@UtilityClass
public class RequestMapper {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static Request toRequest(EventInternalDto event, Long userId) {
        return Request.builder()
                .eventId(event.getId())
                .requesterId(userId)
                .createdAt(LocalDateTime.now())
                .status(isStatusConfirmed(event) ? RequestStatus.CONFIRMED : RequestStatus.PENDING)
                .build();
    }

    public static ParticipationRequestDto toRequestDto(Request request) {
        return ParticipationRequestDto.builder()
                .id(request.getId())
                .created(request.getCreatedAt().format(FORMATTER))
                .eventId(request.getEventId())
                .requesterId(request.getRequesterId())
                .status(request.getStatus().name())
                .build();
    }

    public static List<ParticipationRequestDto> toRequestDto(List<Request> requests) {
        return requests.stream().map(RequestMapper::toRequestDto).toList();
    }

    private boolean isStatusConfirmed(EventInternalDto event) {
        return !event.getRequestModeration() || event.getParticipantLimit() == 0;
    }
}
