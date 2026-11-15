package ru.practicum.main.entity.comment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import ru.practicum.main.client.EventClient;
import ru.practicum.main.client.UserClient;
import ru.practicum.main.dto.comment.CommentDto;
import ru.practicum.main.dto.comment.NewCommentDto;
import ru.practicum.main.dto.comment.UpdateCommentDto;
import ru.practicum.main.dto.comment.UserCommentAdminDto;
import ru.practicum.main.dto.event.EventInternalDto;
import ru.practicum.main.dto.user.UserInternalDto;
import ru.practicum.main.entity.event.EventState;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;
    @Mock
    private UserClient userClient;
    @Mock
    private EventClient eventClient;

    @InjectMocks
    private CommentServiceImpl commentService;

    private UserInternalDto testUser;
    private EventInternalDto testEvent;
    private Comment testComment;

    @BeforeEach
    void setUp() {
        testUser = UserInternalDto.builder()
                .id(1L)
                .name("User")
                .commentsCount(0)
                .adminWarnings(0)
                .build();
        testEvent = EventInternalDto.builder()
                .id(10L)
                .state(EventState.PUBLISHED)
                .build();
        testComment = Comment.builder()
                .id(20L)
                .text("Text")
                .authorId(1L)
                .authorName("User")
                .eventId(10L)
                .created(LocalDateTime.now().minusDays(1))
                .adminWarning(false)
                .build();
    }

    @Test
    void createComment_whenValid_thenSuccess() {
        NewCommentDto dto = new NewCommentDto("Текст", 10L);
        when(userClient.getUser(1L)).thenReturn(testUser);
        when(eventClient.getEvent(10L)).thenReturn(testEvent);
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            comment.setId(100L);
            comment.setCreated(LocalDateTime.now());
            return comment;
        });

        CommentDto result = commentService.createComment(dto, 1L);

        assertEquals(100L, result.getId());
        assertEquals("Текст", result.getText());
        verify(userClient).updateCommentCount(1L, 1);
    }

    @Test
    void createComment_whenUserIsBanned_thenThrowsConflictException() {
        testUser.setBannedUntil(LocalDateTime.now().plusDays(1));
        when(userClient.getUser(1L)).thenReturn(testUser);

        assertThrows(ConflictException.class,
                () -> commentService.createComment(new NewCommentDto("Текст", 10L), 1L));
        verify(commentRepository, never()).save(any());
    }

    @Test
    void createComment_whenBanExpired_thenSuccess() {
        testUser.setBannedUntil(LocalDateTime.now().minusDays(1));
        when(userClient.getUser(1L)).thenReturn(testUser);
        when(eventClient.getEvent(10L)).thenReturn(testEvent);
        when(commentRepository.save(any(Comment.class))).thenAnswer(invocation -> {
            Comment comment = invocation.getArgument(0);
            comment.setCreated(LocalDateTime.now());
            return comment;
        });

        assertDoesNotThrow(() -> commentService.createComment(new NewCommentDto("Текст", 10L), 1L));
    }

    @Test
    void updateComment_whenValid_thenSuccess() {
        UpdateCommentDto dto = new UpdateCommentDto();
        dto.setCommentId(20L);
        dto.setUserId(1L);
        dto.setText("Новый текст");
        when(commentRepository.findById(20L)).thenReturn(Optional.of(testComment));
        when(userClient.getUser(1L)).thenReturn(testUser);

        CommentDto result = commentService.updateComment(dto);

        assertEquals("Новый текст", result.getText());
        assertEquals("Новый текст", testComment.getText());
    }

    @Test
    void updateComment_whenNotAuthor_thenThrowsConflictException() {
        UpdateCommentDto dto = new UpdateCommentDto();
        dto.setCommentId(20L);
        dto.setUserId(999L);
        dto.setText("Новый текст");
        when(commentRepository.findById(20L)).thenReturn(Optional.of(testComment));
        when(userClient.getUser(999L)).thenReturn(UserInternalDto.builder().id(999L).build());

        assertThrows(ConflictException.class, () -> commentService.updateComment(dto));
    }

    @Test
    void deleteComment_whenValid_thenCallsDelete() {
        when(commentRepository.findById(20L)).thenReturn(Optional.of(testComment));

        commentService.deleteComment(20L, 1L);

        verify(commentRepository).delete(testComment);
        verify(userClient).updateCommentCount(1L, -1);
    }

    @Test
    void deleteComment_whenCommentNotFound_thenThrowsNotFoundException() {
        when(commentRepository.findById(100L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> commentService.deleteComment(100L, 1L));
    }

    @Test
    void getComments_whenCalled_thenReturnsList() {
        when(commentRepository.findAllByAuthorId(1L)).thenReturn(List.of(testComment));

        List<CommentDto> result = commentService.getComments(1L);

        assertEquals(1, result.size());
        assertEquals("Text", result.getFirst().getText());
    }

    @Test
    void giveWarning_whenValid_thenDelegatesToUserService() {
        UserCommentAdminDto expected = UserCommentAdminDto.builder()
                .authorId(1L)
                .adminWarnCount(1)
                .build();
        when(commentRepository.findById(20L)).thenReturn(Optional.of(testComment));
        when(userClient.addWarning(1L)).thenReturn(expected);

        UserCommentAdminDto result = commentService.giveWarning(20L);

        assertEquals(1, result.getAdminWarnCount());
        assertTrue(testComment.isAdminWarning());
    }

    @Test
    void giveWarning_whenAlreadyReceived_thenThrowConflictException() {
        testComment.setAdminWarning(true);
        when(commentRepository.findById(20L)).thenReturn(Optional.of(testComment));

        assertThrows(ConflictException.class, () -> commentService.giveWarning(20L));
    }

    @Test
    void adminDelete_whenValid_thenCallsDelete() {
        when(commentRepository.findById(20L)).thenReturn(Optional.of(testComment));

        commentService.adminDelete(20L);

        verify(commentRepository).delete(testComment);
        verify(userClient).updateCommentCount(1L, -1);
    }

    @Test
    void getEventComments_whenCommentsExist_thenReturnDtoList() {
        when(eventClient.getEvent(10L)).thenReturn(testEvent);
        when(commentRepository.findAllByEventId(10L)).thenReturn(List.of(testComment));

        List<CommentDto> result = commentService.getEventComments(10L);

        assertEquals(1, result.size());
        assertEquals(testComment.getId(), result.getFirst().getId());
    }

    @Test
    void getEventComments_whenEventNotFound_thenThrowsNotFoundException() {
        when(eventClient.getEvent(1L)).thenThrow(new NotFoundException("Event not found"));

        assertThrows(NotFoundException.class, () -> commentService.getEventComments(1L));
        verify(commentRepository, never()).findAllByEventId(any());
    }

    @Test
    void getComment_whenCommentExists_thenReturnDto() {
        when(commentRepository.findById(20L)).thenReturn(Optional.of(testComment));

        CommentDto result = commentService.getComment(20L);

        assertNotNull(result);
        assertEquals(testComment.getText(), result.getText());
    }

    @Test
    void getComment_whenCommentDoesNotExist_thenThrowException() {
        when(commentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> commentService.getComment(99L));
    }

    @Test
    void searchComments_whenMatchesFound_thenReturnDtoList() {
        Pageable pageable = PageRequest.of(0, 10);
        when(commentRepository.findAllByTextContainsIgnoreCase("Тест", pageable))
                .thenReturn(List.of(testComment));

        List<CommentDto> result = commentService.searchComments("Тест", pageable);

        assertEquals(1, result.size());
        assertEquals(testComment.getText(), result.getFirst().getText());
    }

    @Test
    void searchComments_whenNoMatches_thenReturnEmptyList() {
        Pageable pageable = PageRequest.of(0, 10);
        when(commentRepository.findAllByTextContainsIgnoreCase("НичегоНеНайдено", pageable))
                .thenReturn(Collections.emptyList());

        List<CommentDto> result = commentService.searchComments("НичегоНеНайдено", pageable);

        assertTrue(result.isEmpty());
    }
}
