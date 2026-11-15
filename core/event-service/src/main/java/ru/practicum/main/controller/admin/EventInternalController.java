package ru.practicum.main.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.main.dto.event.EventInternalDto;
import ru.practicum.main.entity.event.EventService;

@RestController
@RequestMapping("/internal/events")
@RequiredArgsConstructor
public class EventInternalController {

    private final EventService eventService;

    @GetMapping("/{eventId}")
    public EventInternalDto getEvent(@PathVariable Long eventId) {
        return eventService.getInternalEvent(eventId);
    }
}
