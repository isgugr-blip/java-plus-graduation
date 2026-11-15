package ru.practicum.main.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.main.dto.event.EventRequestsCountDto;
import ru.practicum.main.entity.request.RequestStatus;

import java.util.List;
import java.util.Set;

@FeignClient(name = "request-service", path = "/internal/requests")
public interface RequestClient {

    @PostMapping("/counts")
    List<EventRequestsCountDto> countByEventIdsAndStatus(@RequestBody Set<Long> eventIds,
                                                         @RequestParam RequestStatus status);
}
