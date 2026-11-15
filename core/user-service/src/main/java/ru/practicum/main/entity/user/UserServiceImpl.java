package ru.practicum.main.entity.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.main.dto.comment.UserCommentAdminDto;
import ru.practicum.main.dto.user.NewUserRequest;
import ru.practicum.main.dto.user.UserDto;
import ru.practicum.main.dto.user.UserInternalDto;
import ru.practicum.main.dto.user.UserParamDto;
import ru.practicum.main.exception.ConflictException;
import ru.practicum.main.exception.NotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserDto saveUser(NewUserRequest userRequest) {
        if (userRepository.existsByMail(userRequest.getEmail())) {
            throw new ConflictException("User with this email already exists");
        }

        User savedUser = userRepository.save(UserMapper.toUser(userRequest));
        log.info("Новый пользователь {}", savedUser);

        return UserMapper.toUserDto(savedUser);
    }

    @Override
    public List<UserDto> getUsers(UserParamDto userParamDto) {
        if (userParamDto.getIds() != null && !userParamDto.getIds().isEmpty()) {
            List<Long> ids = userParamDto.getIds().stream().toList();
            return UserMapper.toUserDtos(userRepository.findAllByIdIn(ids));
        }

        int page = userParamDto.getFrom() / userParamDto.getSize();
        int pageSize = userParamDto.getSize();
        List<User> users = userRepository.findAll(PageRequest.of(page, pageSize)).getContent();

        return UserMapper.toUserDtos(users);
    }

    @Override
    public List<UserInternalDto> getInternalUsers(Set<Long> userIds) {
        return userRepository.findAllByIdIn(userIds.stream().toList()).stream()
                .map(UserMapper::toInternalDto)
                .toList();
    }

    @Override
    @Transactional
    public void deleteUserById(Long userId) {
        checkUserExist(userId);
        userRepository.deleteById(userId);
    }

    @Override
    public User findById(Long userId) {
        return userRepository.findById(userId).orElseThrow(
                () -> new NotFoundException("User with id: " + userId + " does not exist"));
    }

    @Override
    public UserInternalDto getInternalUser(Long userId) {
        return UserMapper.toInternalDto(findById(userId));
    }

    @Override
    public void checkUserExist(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User with id: " + userId + " does not exist");
        }
    }

    @Override
    @Transactional
    public UserCommentAdminDto updateUserBan(Long userId, LocalDateTime banDate) {
        User user = findById(userId);
        return banDate != null ? banUser(user, banDate) : unbanUser(user);
    }

    @Override
    @Transactional
    public UserInternalDto updateCommentCount(Long userId, int delta) {
        User user = findById(userId);
        user.setCommentsCount(user.getCommentsCount() + delta);
        setUserRank(user);
        return UserMapper.toInternalDto(user);
    }

    @Override
    @Transactional
    public UserCommentAdminDto addWarning(Long userId) {
        User user = findById(userId);
        if (user.isWarningsLimitExceeded()) {
            banCheck(user);
            user.setAdminWarnings(0);
            user.setBannedUntil(LocalDateTime.now().plusMonths(6));
        } else {
            user.setAdminWarnings(user.getAdminWarnings() + 1);
        }
        return UserMapper.toUserCommentAdminDto(user);
    }

    private static UserCommentAdminDto unbanUser(User user) {
        if (!user.isBanned()) {
            throw new ConflictException("User not banned");
        }

        log.info("Пользователь разблокирован:{}", user);
        user.setBannedUntil(null);
        return UserMapper.toUserCommentAdminDto(user);
    }

    private static UserCommentAdminDto banUser(User user, LocalDateTime banDate) {
        if (user.isBanned()) {
            throw new ConflictException("User already banned");
        }

        log.info("Пользователь получил блокировку до {}: {}", banDate, user);
        user.setBannedUntil(banDate);
        return UserMapper.toUserCommentAdminDto(user);
    }

    private static void setUserRank(User user) {
        int commentsCount = user.getCommentsCount();

        if (commentsCount == 1 && user.getRank() == CommentsRank.REGULAR) {
            user.setRank(CommentsRank.NOVICE);
        }
        if (commentsCount == 2) {
            user.setRank(CommentsRank.REGULAR);
        }
        if (commentsCount > 2 && user.getRank() == CommentsRank.REGULAR) {
            user.setRank(CommentsRank.VETERAN);
        }
    }

    private static void banCheck(User user) {
        if (user.getBannedUntil() != null && user.getBannedUntil().isBefore(LocalDateTime.now())) {
            user.setBannedUntil(null);
        }
        if (user.isBanned()) {
            throw new ConflictException("You have been banned!");
        }
    }
}
