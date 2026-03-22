package com.student.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.student.platform.dto.LoginRequest;
import com.student.platform.dto.RegisterRequest;
import com.student.platform.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebMvcTest(AuthController.class)
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private LoginRequest loginRequest;
    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");

        registerRequest = new RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setPassword("password123");
        registerRequest.setEmail("newuser@example.com");
        registerRequest.setPhone("13800138000");
        registerRequest.setNickname("New User");
    }

    @Test
    void testLogin() throws Exception {
        // Mock the userService.login method
        when(userService.login(any(LoginRequest.class)))
                .thenReturn(new com.student.platform.dto.AuthResponse(
                        "test-token-123",
                        "Bearer",
                        1L,
                        "testuser",
                        "STUDENT",
                        new com.student.platform.dto.AuthResponse.UserInfo(1L, "testuser", "STUDENT"),
                        System.currentTimeMillis() + 3600000
                ));

        // Perform the POST request
        mockMvc.perform(MockMvcRequestBuilders.post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.token").exists());
    }

    @Test
    void testRegister() throws Exception {
        // Mock the userService.register method
        when(userService.register(any(RegisterRequest.class)))
                .thenReturn(new com.student.platform.dto.AuthResponse(
                        "test-token-123",
                        "Bearer",
                        2L,
                        "newuser",
                        "STUDENT",
                        new com.student.platform.dto.AuthResponse.UserInfo(2L, "newuser", "STUDENT"),
                        System.currentTimeMillis() + 3600000
                ));

        // Perform the POST request
        mockMvc.perform(MockMvcRequestBuilders.post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.token").exists());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testValidate() throws Exception {
        // Perform the GET request
        mockMvc.perform(MockMvcRequestBuilders.get("/auth/validate"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.code").value(200))
                .andExpect(MockMvcResultMatchers.jsonPath("$.data.username").value("testuser"));
    }
}
