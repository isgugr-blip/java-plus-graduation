package ru.practicum.main.entity.user;

import lombok.experimental.UtilityClass;
import ru.practicum.main.dto.comment.UserCommentAdminDto;
import ru.practicum.main.dto.user.NewUserRequest;
import ru.practicum.main.dto.user.UserDto;
import ru.practicum.main.dto.user.UserInternalDto;
import ru.practicum.main.dto.user.UserShortDto;

import java.time.format.DateTimeFormatter;
import java.util.List;

@UtilityClass
public class UserMapper {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    static User toUser(final NewUserRequest newUserRequest) {
        return User.builder()
                .mail(newUserRequest.getEmail())
                .name(newUserRequest.getName())
                .rank(CommentsRank.NOVICE)
                .build();
    }

    static UserDto toUserDto(final User user) {
        return UserDto.builder()
                .id(user.getId())
                .email(user.getMail())
                .name(user.getName())
                .build();
    }

    static List<UserDto> toUserDtos(final List<User> users) {
        return users.stream()
                .map(UserMapper::toUserDto)
                .toList();
    }

    public static UserShortDto toShortDto(final User user) {
        return UserShortDto.builder()
                .name(user.getName())
                .id(user.getId())
                .build();
    }

    public static UserInternalDto toInternalDto(final User user) {
        return UserInternalDto.builder()
                .id(user.getId())
                .name(user.getName())
                .commentsCount(user.getCommentsCount())
                .adminWarnings(user.getAdminWarnings())
                .bannedUntil(user.getBannedUntil())
                .build();
    }

    public static UserCommentAdminDto toUserCommentAdminDto(final User user) {
        return UserCommentAdminDto.builder()
                .authorId(user.getId())
                .authorName(user.getName())
                .adminWarnCount(user.getAdminWarnings())
                .bannedUntil(user.getBannedUntil() != null ? user.getBannedUntil().format(FORMATTER) : null)
                .build();
    }
}
