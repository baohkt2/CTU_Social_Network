package com.ctuconnect.controller;

import com.ctuconnect.dto.AuthResponse;
import com.ctuconnect.dto.LoginRequest;
import com.ctuconnect.dto.RegisterRequest;
import com.ctuconnect.dto.UserDTO;
import com.ctuconnect.security.JwtAuthenticationFilter;
import com.ctuconnect.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void register_returnsOkAndStripsTokensFromBody() throws Exception {
        UserDTO user = UserDTO.builder().id("1").email("a@student.ctu.edu.vn").username("user1").build();
        when(authService.register(any(RegisterRequest.class))).thenReturn(
                AuthResponse.builder()
                        .accessToken("access")
                        .refreshToken("refresh")
                        .tokenType("Bearer")
                        .user(user)
                        .build());

        RegisterRequest body = new RegisterRequest();
        body.setEmail("a@student.ctu.edu.vn");
        body.setUsername("user1");
        body.setPassword("Abcd1234@");
        body.setRole("USER");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(nullValue()))
                .andExpect(jsonPath("$.refreshToken").value(nullValue()))
                .andExpect(jsonPath("$.user.email").value("a@student.ctu.edu.vn"));
    }

    @Test
    void login_returnsOkAndStripsTokensFromBody() throws Exception {
        UserDTO user = UserDTO.builder().id("2").email("b@ctu.edu.vn").username("user2").build();
        when(authService.login(any(LoginRequest.class))).thenReturn(
                AuthResponse.builder()
                        .accessToken("access")
                        .refreshToken("refresh")
                        .tokenType("Bearer")
                        .user(user)
                        .build());

        LoginRequest body = new LoginRequest();
        body.setIdentifier("b@ctu.edu.vn");
        body.setPassword("Abcd1234@");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(nullValue()))
                .andExpect(jsonPath("$.refreshToken").value(nullValue()))
                .andExpect(jsonPath("$.user.username").value("user2"));
    }
}
