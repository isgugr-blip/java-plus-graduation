package ru.practicum.main.controller.private_api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.main.dto.event.EventRequestsCountDto;
import ru.practicum.main.entity.request.RequestService;
import ru.practicum.main.entity.request.RequestStatus;

import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RequestInternalController.class)
class RequestInternalControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private RequestService requestService;

    @Test
    void getCounts_shouldReturnBatchCounts() throws Exception {
        when(requestService.countByEventIdsAndStatus(Set.of(1L, 2L), RequestStatus.CONFIRMED))
                .thenReturn(List.of(new EventRequestsCountDto(1L, 3L)));

        mvc.perform(post("/internal/requests/counts")
                        .param("status", "CONFIRMED")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1,2]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventId").value(1))
                .andExpect(jsonPath("$[0].count").value(3));
    }
}
