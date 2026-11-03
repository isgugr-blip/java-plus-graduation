package ru.practicum.main.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.main.dto.comment.UserCommentAdminDto;
import ru.practicum.main.dto.user.UserInternalDto;

import java.util.List;
import java.util.Set;

@FeignClient(name = "user-service", path = "/internal/users")
public interface UserClient {

    @GetMapping("/{userId}")
    UserInternalDto getUser(@PathVariable Long userId);

    @PostMapping("/batch")
    List<UserInternalDto> getUsers(@RequestBody Set<Long> userIds);

    @PostMapping("/{userId}/comment-count")
    UserInternalDto updateCommentCount(@PathVariable Long userId, @RequestParam int delta);

    @PostMapping("/{userId}/warnings")
    UserCommentAdminDto addWarning(@PathVariable Long userId);
}
