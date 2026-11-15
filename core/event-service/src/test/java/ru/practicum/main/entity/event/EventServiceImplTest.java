package ru.practicum.main.entity.event;

import com.querydsl.core.types.Predicate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import ru.practicum.main.client.UserClient;
import ru.practicum.main.dto.category.CategoryDto;
import ru.practicum.main.dto.event.EventAdminParamDto;
import ru.practicum.main.dto.event.EventFullDto;
import ru.practicum.main.dto.event.Location;
import ru.practicum.main.dto.event.NewEventDto;
import ru.practicum.main.dto.event.UpdateEventAdminRequest;
import ru.practicum.main.dto.user.UserInternalDto;
import ru.practicum.main.entity.category.Category;
import ru.practicum.main.entity.category.CategoryService;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;
import ru.practicum.stat.client.StatClient;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;
    @Mock
    private CategoryService categoryService;
    @Mock
    private UserClient userClient;
    @Mock
    private StatClient statClient;
    @Mock
    private RequestClientAdapter requestClient;

    @InjectMocks
    private EventServiceImpl eventService;

    @Test
    void getEventsByAdmin_shouldReturnFilteredEvents() {
        EventAdminParamDto params = EventAdminParamDto.builder()
                .users(List.of(1L))
                .states(List.of("PUBLISHED"))
                .from(0)
                .size(10)
                .build();
        Event event = event(EventState.PUBLISHED);

        when(eventRepository.findAll(any(Predicate.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(event)));
        when(statClient.getViewStats(any())).thenReturn(Collections.emptyList());
        when(requestClient.countByEventIdsAndStatus(any(), any())).thenReturn(Collections.emptyList());

        List<EventFullDto> result = eventService.getEventsByAdmin(params);

        assertEquals(1, result.size());
        verify(statClient).getViewStats(any());
    }

    @Test
    void updateEventByAdmin_publishEvent_shouldSetPublished() {
        Event event = event(EventState.PENDING);
        UpdateEventAdminRequest request = UpdateEventAdminRequest.builder()
                .stateAction(AdminStateAction.PUBLISH_EVENT)
                .build();

        when(eventRepository.findWithRelationsById(1L)).thenReturn(Optional.of(event));
        when(statClient.getViewStats(any())).thenReturn(Collections.emptyList());
        when(requestClient.countByEventIdsAndStatus(any(), any())).thenReturn(Collections.emptyList());

        EventFullDto result = eventService.updateEventByAdmin(1L, request);

        assertEquals("PUBLISHED", result.getState());
        assertNotNull(result.getPublishedOn());
    }

    @Test
    void updateEventByAdmin_publishEvent_wrongState_shouldThrowConflict() {
        Event event = event(EventState.PUBLISHED);
        UpdateEventAdminRequest request = UpdateEventAdminRequest.builder()
                .stateAction(AdminStateAction.PUBLISH_EVENT)
                .build();
        when(eventRepository.findWithRelationsById(1L)).thenReturn(Optional.of(event));

        assertThrows(ConflictException.class, () -> eventService.updateEventByAdmin(1L, request));
    }

    @Test
    void getPublishedEventById_notFound_shouldThrowNotFoundException() {
        when(eventRepository.findByIdAndState(999L, EventState.PUBLISHED)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> eventService.getPublishedEventById(999L, null));
    }

    @Test
    void create_shouldReturnCreatedEvent() {
        UserInternalDto user = UserInternalDto.builder().id(1L).name("user").build();
        CategoryDto categoryDto = CategoryDto.builder().id(1L).name("cat").build();
        String futureDate = LocalDateTime.now().plusDays(1)
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        NewEventDto newEvent = NewEventDto.builder()
                .annotation("annotation")
                .category(1L)
                .description("description")
                .eventDate(futureDate)
                .title("title")
                .location(Location.builder().lat(1.0f).lon(1.0f).build())
                .build();

        when(userClient.getUser(1L)).thenReturn(user);
        when(categoryService.findById(1L)).thenReturn(categoryDto);
        when(eventRepository.save(any())).thenAnswer(inv -> {
            Event e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        EventFullDto result = eventService.create(1L, newEvent);

        assertEquals("title", result.getTitle());
        assertEquals("PENDING", result.getState());
        verify(eventRepository).save(any());
    }

    private Event event(EventState state) {
        return Event.builder()
                .id(1L)
                .state(state)
                .eventDate(LocalDateTime.now().plusDays(1))
                .createdOn(LocalDateTime.now())
                .location(new Location(1.1f, 1.1f))
                .category(Category.builder().id(1L).name("cat").build())
                .initiatorId(1L)
                .initiatorName("user")
                .build();
    }
}
