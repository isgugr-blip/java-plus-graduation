package ru.practicum.main.controller.admin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.main.dto.event.EventInternalDto;
import ru.practicum.main.entity.event.EventService;
import ru.practicum.main.entity.event.EventState;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventInternalController.class)
class EventInternalControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private EventService eventService;

    @Test
    void getEvent_shouldReturnParticipationData() throws Exception {
        when(eventService.getInternalEvent(1L)).thenReturn(EventInternalDto.builder()
                .id(1L)
                .initiatorId(2L)
                .state(EventState.PUBLISHED)
                .participantLimit(10)
                .requestModeration(true)
                .build());

        mvc.perform(get("/internal/events/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.initiatorId").value(2))
                .andExpect(jsonPath("$.state").value("PUBLISHED"))
                .andExpect(jsonPath("$.participantLimit").value(10));
    }
}
