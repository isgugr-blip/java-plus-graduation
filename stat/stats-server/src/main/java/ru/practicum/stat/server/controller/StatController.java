package ru.practicum.stat.server.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.practicum.stat.dto.EndpointHit;
import ru.practicum.stat.dto.ParamDto;
import ru.practicum.stat.dto.ViewStats;
import ru.practicum.stat.server.service.StatService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class StatController {

    private final StatService statService;

    @PostMapping("/hit")
    @ResponseStatus(HttpStatus.CREATED)
    public void create(@RequestBody @Valid EndpointHit endpointHit) {
        statService.saveHit(endpointHit);
    }

    @GetMapping("/stats")
    public List<ViewStats> getViewStats(@ModelAttribute @Valid ParamDto paramDto) {
        return statService.getStats(paramDto);
    }
}
