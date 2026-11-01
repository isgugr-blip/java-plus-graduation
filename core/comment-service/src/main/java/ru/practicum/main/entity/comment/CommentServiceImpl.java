package ru.practicum.main.entity.comment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import ru.practicum.main.exception.ServiceUnavailableException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@Transactional(readOnly = true)
@Slf4j
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final UserClient userClient;
    private final EventClient eventClient;

    @Override
    @Transactional
    public CommentDto createComment(NewCommentDto newCommentDto, Long userId) {
        UserInternalDto user = userClient.getUser(userId);
        banCheck(user);
        EventInternalDto event = eventClient.getEvent(newCommentDto.getEventId());
        if (event.getState() != EventState.PUBLISHED) {
            throw new NotFoundException("Event with id=" + event.getId() + " was not found");
        }

        Comment comment = commentRepository.save(CommentMapper.toComment(newCommentDto, user, event.getId()));
        userClient.updateCommentCount(userId, 1);
        CommentDto result = CommentMapper.toCommentDto(comment);
        log.info("Новый комментарий на событие: {}", CommentMapper.toLogComment(result));
        return result;
    }

    @Override
    @Transactional
    public CommentDto updateComment(UpdateCommentDto updateCommentDto) {
        Comment comment = getById(updateCommentDto.getCommentId());
        UserInternalDto user = userClient.getUser(updateCommentDto.getUserId());
        checkAuthor(comment, updateCommentDto.getUserId());
        banCheck(user);

        comment.setText(updateCommentDto.getText());
        CommentDto result = CommentMapper.toCommentDto(comment);
        log.info("Обновлен комментарий на событие: {}", CommentMapper.toLogComment(result));
        return result;
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = getById(commentId);
        checkAuthor(comment, userId);
        commentRepository.delete(comment);
        userClient.updateCommentCount(comment.getAuthorId(), -1);
        log.info("Комментарий id: {} удален пользователем id: {}", commentId, userId);
    }

    @Override
    public List<CommentDto> getComments(Long userId) {
        return CommentMapper.toCommentDto(commentRepository.findAllByAuthorId(userId));
    }

    @Override
    @Transactional
    public UserCommentAdminDto giveWarning(Long commentId) {
        Comment comment = getById(commentId);
        if (comment.isAdminWarning()) {
            throw new ConflictException(String.format(
                    "User with id %s already received warning for comment with id %s",
                    comment.getAuthorId(), comment.getId()));
        }
        comment.setAdminWarning(true);
        return userClient.addWarning(comment.getAuthorId());
    }

    @Override
    @Transactional
    public void adminDelete(Long commentId) {
        Comment comment = getById(commentId);
        CommentDto dto = CommentMapper.toCommentDto(comment);
        commentRepository.delete(comment);
        userClient.updateCommentCount(comment.getAuthorId(), -1);
        log.info("Комментарий удален администратором: {}", CommentMapper.toLogComment(dto));
    }

    @Override
    public List<CommentDto> getEventComments(Long eventId) {
        try {
            eventClient.getEvent(eventId);
        } catch (ServiceUnavailableException e) {
            log.warn("Событие не проверено из-за недоступности event-service: {}", e.getMessage());
        }
        return CommentMapper.toCommentDto(commentRepository.findAllByEventId(eventId));
    }

    @Override
    public CommentDto getComment(Long commentId) {
        return CommentMapper.toCommentDto(getById(commentId));
    }

    @Override
    public List<CommentDto> searchComments(String text, Pageable pageable) {
        List<Comment> comments = commentRepository.findAllByTextContainsIgnoreCase(text, pageable);
        if (comments.isEmpty()) {
            return Collections.emptyList();
        }
        return CommentMapper.toCommentDto(comments);
    }

    @Override
    public Comment getById(Long id) {
        return commentRepository.findById(id).orElseThrow(
                () -> new NotFoundException("Comment not found with id: " + id));
    }

    private static void banCheck(UserInternalDto user) {
        LocalDateTime bannedUntil = user.getBannedUntil();
        if (bannedUntil != null && bannedUntil.isAfter(LocalDateTime.now())) {
            throw new ConflictException("You have been banned!");
        }
    }

    private static void checkAuthor(Comment comment, Long userId) {
        if (!comment.getAuthorId().equals(userId)) {
            throw new ConflictException("It's not your own comment!");
        }
    }
}
