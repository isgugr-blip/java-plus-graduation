package ru.practicum.main.controller.admin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.main.dto.user.UserInternalDto;
import ru.practicum.main.entity.user.UserService;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserInternalController.class)
class UserInternalControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private UserService userService;

    @Test
    void getUser_shouldReturnInternalData() throws Exception {
        when(userService.getInternalUser(1L)).thenReturn(UserInternalDto.builder()
                .id(1L)
                .name("User")
                .commentsCount(2)
                .build());

        mvc.perform(get("/internal/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("User"))
                .andExpect(jsonPath("$.commentsCount").value(2));
    }

    @Test
    void updateCommentCount_shouldAcceptPost() throws Exception {
        when(userService.updateCommentCount(1L, 1)).thenReturn(UserInternalDto.builder()
                .id(1L)
                .commentsCount(1)
                .build());

        mvc.perform(post("/internal/users/1/comment-count").param("delta", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentsCount").value(1));

        verify(userService).updateCommentCount(1L, 1);
    }
}
