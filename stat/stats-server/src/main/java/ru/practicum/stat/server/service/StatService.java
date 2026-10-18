package ru.practicum.stat.server.service;

import ru.practicum.stat.dto.EndpointHit;
import ru.practicum.stat.dto.ParamDto;
import ru.practicum.stat.dto.ViewStats;

import java.util.List;

public interface StatService {

    void saveHit(EndpointHit dto);

    List<ViewStats> getStats(ParamDto paramDto);
}