package ru.practicum.main.entity.comment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.main.dto.comment.CommentDto;
import ru.practicum.main.dto.comment.LogComment;
import ru.practicum.main.dto.comment.NewCommentDto;
import ru.practicum.main.dto.user.UserInternalDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class CommentMapperTest {

    private UserInternalDto testUser;
    private LocalDateTime fixedTime;

    @BeforeEach
    void setUp() {
        fixedTime = LocalDateTime.of(2026, 7, 2, 19, 0, 0);
        testUser = UserInternalDto.builder().id(1L).name("Иван Иванов").build();
    }

    @Test
    void toComment_whenValid_thenMapCorrectly() {
        NewCommentDto newCommentDto = new NewCommentDto("Текст тестового комментария", 10L);

        Comment result = CommentMapper.toComment(newCommentDto, testUser, 10L);

        assertNotNull(result);
        assertEquals("Текст тестового комментария", result.getText());
        assertEquals(1L, result.getAuthorId());
        assertEquals("Иван Иванов", result.getAuthorName());
        assertEquals(10L, result.getEventId());
    }

    @Test
    void toCommentDto_whenUpdatedIsNull_thenMapCorrectly() {
        Comment comment = Comment.builder()
                .id(100L)
                .text("Привет")
                .authorId(1L)
                .authorName("Иван Иванов")
                .eventId(10L)
                .created(fixedTime)
                .build();

        CommentDto result = CommentMapper.toCommentDto(comment);

        assertEquals(100L, result.getId());
        assertEquals(1L, result.getAuthorId());
        assertEquals(10L, result.getEventId());
        assertEquals("Иван Иванов", result.getAuthorName());
        assertEquals("2026-07-02 19:00:00", result.getCreated());
        assertNull(result.getUpdated());
    }

    @Test
    void toCommentDto_whenUpdatedIsPresent_thenFormatUpdatedDate() {
        Comment comment = Comment.builder()
                .id(100L)
                .text("Привет")
                .authorId(1L)
                .authorName("Иван Иванов")
                .eventId(10L)
                .created(fixedTime)
                .updated(fixedTime.plusHours(2))
                .build();

        CommentDto result = CommentMapper.toCommentDto(comment);

        assertEquals("2026-07-02 21:00:00", result.getUpdated());
    }

    @Test
    void toCommentDto_listMethod_thenMapList() {
        Comment comment = Comment.builder()
                .id(100L)
                .text("Текст")
                .authorId(1L)
                .authorName("Иван Иванов")
                .eventId(10L)
                .created(fixedTime)
                .build();

        List<CommentDto> result = CommentMapper.toCommentDto(List.of(comment));

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getId());
    }

    @Test
    void toLogComment_whenTextIsLong_thenAbbreviateText() {
        CommentDto dto = CommentDto.builder()
                .id(50L)
                .authorId(1L)
                .eventId(10L)
                .authorName("Иван")
                .text("ОченьДлинныйТекстБольшеДесятиСимволов")
                .created("2026-07-02 19:00:00")
                .build();

        LogComment result = CommentMapper.toLogComment(dto);

        assertEquals(50L, result.getId());
        assertEquals("ОченьДлинн...", result.getText());
    }

    @Test
    void toLogComment_whenTextIsShortOrNull_thenMapWithoutChanges() {
        LogComment shortResult = CommentMapper.toLogComment(
                CommentDto.builder().text("Привет").created("2026").build());
        LogComment nullResult = CommentMapper.toLogComment(
                CommentDto.builder().text(null).created("2026").build());

        assertEquals("Привет", shortResult.getText());
        assertNull(nullResult.getText());
    }
}
