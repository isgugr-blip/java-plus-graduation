package ru.practicum.main.entity.event;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.main.client.RequestClient;
import ru.practicum.main.dto.event.EventRequestsCountDto;
import ru.practicum.main.entity.request.RequestStatus;
import ru.practicum.main.exception.ServiceUnavailableException;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestClientAdapterTest {

    @Mock
    private RequestClient requestClient;

    @InjectMocks
    private RequestClientAdapter adapter;

    @Test
    void countByEventIdsAndStatus_shouldReturnCounts() {
        Set<Long> ids = Set.of(1L);
        when(requestClient.countByEventIdsAndStatus(ids, RequestStatus.CONFIRMED))
                .thenReturn(List.of(new EventRequestsCountDto(1L, 2L)));

        List<EventRequestsCountDto> result = adapter.countByEventIdsAndStatus(ids, RequestStatus.CONFIRMED);

        assertEquals(2L, result.getFirst().count());
    }

    @Test
    void emptyCounts_shouldReturnEmptyListOnFailure() {
        Set<Long> ids = Set.of(1L);
        List<EventRequestsCountDto> result = adapter.emptyCounts(
                ids, RequestStatus.CONFIRMED, new ServiceUnavailableException("Unavailable"));

        assertTrue(result.isEmpty());
    }
}
