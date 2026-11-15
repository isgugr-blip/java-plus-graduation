package ru.practicum.main.controller.private_api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.main.dto.event.EventFullDto;
import ru.practicum.main.dto.event.EventParamDto;
import ru.practicum.main.dto.event.EventShortDto;
import ru.practicum.main.dto.event.NewEventDto;
import ru.practicum.main.dto.event.UpdateEventUserRequest;
import ru.practicum.main.entity.event.EventService;

import java.util.List;

@RestController
@RequestMapping(path = "/users")
@RequiredArgsConstructor
@Validated
public class EventPrivateController {

    private final EventService eventService;

    @PostMapping("/{userId}/events")
    @ResponseStatus(HttpStatus.CREATED)
    public EventFullDto create(@PathVariable @Positive Long userId,
                               @RequestBody @Valid NewEventDto dto) {
        return eventService.create(userId, dto);
    }

    @GetMapping("/{userId}/events/{eventId}")
    public EventFullDto getById(@PathVariable @Positive Long userId,
                                @PathVariable @Positive Long eventId) {
        return eventService.getById(userId, eventId);
    }

    @GetMapping("/{userId}/events")
    public List<EventShortDto> getUserEvents(@PathVariable @Positive Long userId,
                                             @RequestParam(defaultValue = "0") @PositiveOrZero int from,
                                             @RequestParam(defaultValue = "10") @Positive int size) {
        Pageable pageable = PageRequest.of(from / size, size);
        return eventService.getUserEvents(userId, pageable);
    }

    @PatchMapping("/{userId}/events/{eventId}")
    public EventFullDto updateEventByCreatorId(@PathVariable @Positive Long userId,
                                               @PathVariable @Positive Long eventId,
                                               @RequestBody @Valid UpdateEventUserRequest request) {
        return eventService.updateEventByCreatorId(new EventParamDto(userId, eventId), request);
    }
}
