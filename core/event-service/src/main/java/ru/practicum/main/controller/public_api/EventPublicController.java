package ru.practicum.main.controller.public_api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.main.dto.event.EventFullDto;
import ru.practicum.main.dto.event.EventPublicParamDto;
import ru.practicum.main.dto.event.EventShortDto;
import ru.practicum.main.entity.event.EventService;

import java.util.List;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Validated
public class EventPublicController {

    private static final String USER_ID_HEADER = "X-EWM-USER-ID";

    private final EventService eventService;

    @GetMapping
    public List<EventShortDto> getEvents(@ModelAttribute @Valid EventPublicParamDto params) {
        return eventService.getPublishedEvents(params);
    }

    @GetMapping("/{id}")
    public EventFullDto getEventById(@PathVariable @Positive Long id,
                                     @RequestHeader(USER_ID_HEADER) long userId) {
        return eventService.getPublishedEventById(id, userId);
    }

    @GetMapping("/recommendations")
    public List<EventShortDto> getRecommendations(@RequestHeader(USER_ID_HEADER) long userId,
                                                  @RequestParam(defaultValue = "10") @Positive int maxResults) {
        return eventService.getRecommendations(userId, maxResults);
    }

    @PutMapping("/{eventId}/like")
    public void likeEvent(@PathVariable @Positive Long eventId,
                          @RequestHeader(USER_ID_HEADER) long userId) {
        eventService.likeEvent(userId, eventId);
    }
}
