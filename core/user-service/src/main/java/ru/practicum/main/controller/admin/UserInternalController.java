package ru.practicum.main.controller.admin;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.main.dto.comment.UserCommentAdminDto;
import ru.practicum.main.dto.user.UserInternalDto;
import ru.practicum.main.entity.user.UserService;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class UserInternalController {

    private final UserService userService;

    @GetMapping("/{userId}")
    public UserInternalDto getUser(@PathVariable Long userId) {
        return userService.getInternalUser(userId);
    }

    @PostMapping("/batch")
    public List<UserInternalDto> getUsers(@RequestBody Set<Long> userIds) {
        return userService.getInternalUsers(userIds);
    }

    @PostMapping("/{userId}/comment-count")
    public UserInternalDto updateCommentCount(@PathVariable Long userId, @RequestParam int delta) {
        return userService.updateCommentCount(userId, delta);
    }

    @PostMapping("/{userId}/warnings")
    public UserCommentAdminDto addWarning(@PathVariable Long userId) {
        return userService.addWarning(userId);
    }
}
