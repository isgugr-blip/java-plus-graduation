package ru.practicum.main.entity.event;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.main.client.RequestClient;
import ru.practicum.main.dto.event.EventRequestsCountDto;
import ru.practicum.main.entity.request.RequestStatus;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class RequestClientAdapter {

    private final RequestClient requestClient;

    @Retry(name = "request-service")
    @CircuitBreaker(name = "request-service", fallbackMethod = "emptyCounts")
    public List<EventRequestsCountDto> countByEventIdsAndStatus(Set<Long> eventIds, RequestStatus status) {
        return requestClient.countByEventIdsAndStatus(eventIds, status);
    }

    List<EventRequestsCountDto> emptyCounts(Set<Long> eventIds, RequestStatus status, Throwable error) {
        return List.of();
    }
}
