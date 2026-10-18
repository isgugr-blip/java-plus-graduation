package ru.practicum.stat.client;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class StatsRequest {
    private String startTime;
    private String endTime;
    private List<String> uris;
    private Boolean unique;
}
