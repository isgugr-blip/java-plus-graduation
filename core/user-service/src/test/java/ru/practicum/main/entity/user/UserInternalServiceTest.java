package ru.practicum.main.entity.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.main.dto.comment.UserCommentAdminDto;
import ru.practicum.main.dto.user.UserInternalDto;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserInternalServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void updateCommentCount_shouldUpdateRank() {
        User user = User.builder().id(1L).commentsCount(1).rank(CommentsRank.NOVICE).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserInternalDto result = userService.updateCommentCount(1L, 1);

        assertEquals(2, result.getCommentsCount());
        assertEquals(CommentsRank.REGULAR, user.getRank());
    }

    @Test
    void addWarning_shouldIncrementCounter() {
        User user = User.builder().id(1L).adminWarnings(0).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserCommentAdminDto result = userService.addWarning(1L);

        assertEquals(1, result.getAdminWarnCount());
    }

    @Test
    void addWarning_shouldBanWhenLimitExceeded() {
        User user = User.builder().id(1L).adminWarnings(2).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserCommentAdminDto result = userService.addWarning(1L);

        assertEquals(0, result.getAdminWarnCount());
        assertNotNull(result.getBannedUntil());
    }
}
