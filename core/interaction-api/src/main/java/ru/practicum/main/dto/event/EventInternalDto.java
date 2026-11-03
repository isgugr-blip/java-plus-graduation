package ru.practicum.main.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.practicum.main.entity.event.EventState;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventInternalDto {
    private Long id;
    private Long initiatorId;
    private EventState state;
    private Integer participantLimit;
    private Boolean requestModeration;
}
