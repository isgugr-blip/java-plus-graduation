package ru.practicum.stat.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import ru.practicum.stat.dto.EndpointHit;
import ru.practicum.stat.dto.ViewStats;

import java.util.Collections;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class StatClient {

    private final RestClient restClient;
    private final DiscoveryClient discoveryClient;
    private final String statServiceId;

    public void hit(EndpointHit endpointHit) {
        try {
            restClient.post()
                    .uri(statServerUri()
                            .path("/hit")
                            .build()
                            .toUri())
                    .body(endpointHit)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException | IllegalStateException e) {
            log.error("Stats service unavailable: {}", e.getMessage(), e);
        }
    }

    public List<ViewStats> getViewStats(StatsRequest statsRequest) {
        try {
            return restClient.get()
                    .uri(statServerUri()
                            .path("/stats")
                            .queryParam("start", statsRequest.getStartTime())
                            .queryParam("end", statsRequest.getEndTime())
                            .queryParam("uris", statsRequest.getUris())
                            .queryParam("unique", statsRequest.getUnique())
                            .encode()
                            .build()
                            .toUri())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<ViewStats>>() {
                    });
        } catch (RestClientException | IllegalStateException e) {
            log.error("Stats service unavailable: {}", e.getMessage(), e);
            return  Collections.emptyList();
        }
    }

    private UriComponentsBuilder statServerUri() {
        List<ServiceInstance> instances = discoveryClient.getInstances(statServiceId);
        if (instances.isEmpty()) {
            throw new IllegalStateException("Сервис статистики " + statServiceId + " не найден в реестре сервисов");
        }
        return UriComponentsBuilder.fromUri(instances.getFirst().getUri());
    }
}
