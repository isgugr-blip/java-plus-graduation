package ru.practicum.main.controller.private_api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.main.dto.event.EventRequestStatusUpdateRequest;
import ru.practicum.main.dto.event.EventRequestStatusUpdateResult;
import ru.practicum.main.dto.request.ParticipationRequestDto;
import ru.practicum.main.entity.request.RequestService;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventRequestPrivateController.class)
class EventPrivateControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private RequestService requestService;

    @Test
    void getEventRequestsByCreatorId_shouldReturnRequests() throws Exception {
        ParticipationRequestDto dto = ParticipationRequestDto.builder().id(1L).build();
        when(requestService.getEventRequestsByCreatorId(1L, 2L)).thenReturn(List.of(dto));

        mvc.perform(get("/users/1/events/2/requests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));

        verify(requestService).getEventRequestsByCreatorId(1L, 2L);
    }

    @Test
    void updateEventRequestsStatus_shouldReturnResult() throws Exception {
        EventRequestStatusUpdateRequest request = EventRequestStatusUpdateRequest.builder().build();
        EventRequestStatusUpdateResult result = EventRequestStatusUpdateResult.builder()
                .confirmedRequests(List.of())
                .rejectedRequests(List.of())
                .build();
        when(requestService.updateEventRequestsStatus(any(), any(), any())).thenReturn(result);

        mvc.perform(patch("/users/1/events/2/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(requestService).updateEventRequestsStatus(any(), any(), any());
    }
}
