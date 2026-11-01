package ru.practicum.main.controller.private_api;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.main.dto.event.EventRequestsCountDto;
import ru.practicum.main.entity.request.RequestService;
import ru.practicum.main.entity.request.RequestStatus;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/internal/requests")
@RequiredArgsConstructor
public class RequestInternalController {

    private final RequestService requestService;

    @PostMapping("/counts")
    public List<EventRequestsCountDto> getCounts(@RequestBody Set<Long> eventIds,
                                                 @RequestParam RequestStatus status) {
        return requestService.countByEventIdsAndStatus(eventIds, status);
    }
}
