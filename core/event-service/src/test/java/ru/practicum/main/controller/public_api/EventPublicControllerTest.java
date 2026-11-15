package ru.practicum.main.controller.public_api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.main.dto.event.EventFullDto;
import ru.practicum.main.dto.event.EventPublicParamDto;
import ru.practicum.main.dto.event.EventShortDto;
import ru.practicum.main.entity.event.EventService;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EventPublicController.class)
class EventPublicControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private EventService eventService;

    private EventShortDto shortDto;
    private EventFullDto fullDto;

    @BeforeEach
    void setUp() {
        shortDto = EventShortDto.builder()
                .id(1L)
                .title("Title")
                .build();

        fullDto = EventFullDto.builder()
                .id(1L)
                .title("Title")
                .state("PUBLISHED")
                .build();
    }

    @Test
    void getEvents_shouldReturnEvents() throws Exception {
        when(eventService.getPublishedEvents(any(EventPublicParamDto.class)))
                .thenReturn(List.of(shortDto));

        mvc.perform(get("/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Title"));

        verify(eventService)
                .getPublishedEvents(any(EventPublicParamDto.class));
    }

    @Test
    void getEventById_shouldReturnEvent() throws Exception {
        when(eventService.getPublishedEventById(1L, 7L))
                .thenReturn(fullDto);

        mvc.perform(get("/events/1").header("X-EWM-USER-ID", 7))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Title"));

        verify(eventService)
                .getPublishedEventById(1L, 7L);
    }

    @Test
    void getEventById_withoutUserHeader_shouldReturn400() throws Exception {
        mvc.perform(get("/events/1"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(eventService);
    }

    @Test
    void likeEvent_shouldPassUserFromHeader() throws Exception {
        mvc.perform(put("/events/1/like").header("X-EWM-USER-ID", 7))
                .andExpect(status().isOk());

        verify(eventService).likeEvent(7L, 1L);
    }

    @Test
    void getEventById_whenIdIsNegative_shouldReturn400() throws Exception {
        mvc.perform(get("/events/-1").header("X-EWM-USER-ID", 7))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(eventService);
    }

    @Test
    void getEvents_whenInvalidParams_shouldReturn400() throws Exception {
        mvc.perform(get("/events")
                        .param("from", "-1")
                        .param("size", "0"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(eventService);
    }

    @Test
    void getEvents_whenTextEmpty_shouldReturn400() throws Exception {
        mvc.perform(get("/events")
                        .param("text", ""))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(eventService);
    }
}