package ru.practicum.main.entity.comment;

import lombok.experimental.UtilityClass;
import ru.practicum.main.dto.comment.CommentDto;
import ru.practicum.main.dto.comment.LogComment;
import ru.practicum.main.dto.comment.NewCommentDto;
import ru.practicum.main.dto.user.UserInternalDto;

import java.time.format.DateTimeFormatter;
import java.util.List;

@UtilityClass
public class CommentMapper {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static Comment toComment(NewCommentDto comment, UserInternalDto author, Long eventId) {
        return Comment.builder()
                .text(comment.getText())
                .authorId(author.getId())
                .authorName(author.getName())
                .eventId(eventId)
                .build();
    }

    public static CommentDto toCommentDto(Comment comment) {
        return CommentDto.builder()
                .id(comment.getId())
                .authorId(comment.getAuthorId())
                .eventId(comment.getEventId())
                .authorName(comment.getAuthorName())
                .text(comment.getText())
                .created(comment.getCreated().format(FORMATTER))
                .updated(comment.getUpdated() != null ? comment.getUpdated().format(FORMATTER) : null)
                .build();
    }

    public static List<CommentDto> toCommentDto(List<Comment> comments) {
        return comments.stream().map(CommentMapper::toCommentDto).toList();
    }

    public static LogComment toLogComment(CommentDto comment) {
        return LogComment.builder()
                .id(comment.getId())
                .authorId(comment.getAuthorId())
                .eventId(comment.getEventId())
                .authorName(comment.getAuthorName())
                .text(stringBuilder(comment.getText()))
                .created(comment.getCreated())
                .updated(comment.getUpdated())
                .build();
    }

    private static String stringBuilder(String string) {
        if (string == null) {
            return null;
        }
        return string.length() > 10 ? string.substring(0, 10) + "..." : string;
    }
}
