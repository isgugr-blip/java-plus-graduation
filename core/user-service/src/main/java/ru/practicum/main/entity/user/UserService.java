package ru.practicum.main.entity.user;

import ru.practicum.main.dto.comment.UserCommentAdminDto;
import ru.practicum.main.dto.user.NewUserRequest;
import ru.practicum.main.dto.user.UserDto;
import ru.practicum.main.dto.user.UserParamDto;
import ru.practicum.main.dto.user.UserInternalDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public interface UserService {

    UserDto saveUser(NewUserRequest userRequest);

    List<UserDto> getUsers(UserParamDto userParamDto);

    void deleteUserById(Long userId);

    User findById(Long userId);

    void checkUserExist(Long userId);

    UserCommentAdminDto updateUserBan(Long userId, LocalDateTime banDate);

    UserInternalDto getInternalUser(Long userId);

    List<UserInternalDto> getInternalUsers(Set<Long> userIds);

    UserInternalDto updateCommentCount(Long userId, int delta);

    UserCommentAdminDto addWarning(Long userId);
}
