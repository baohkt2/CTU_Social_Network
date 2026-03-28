package com.ctuconnect.service;

import com.ctuconnect.dto.LoginRequest;
import com.ctuconnect.dto.RegisterRequest;
import com.ctuconnect.dto.AuthResponse;
import com.ctuconnect.entity.EmailVerificationEntity;
import com.ctuconnect.entity.RefreshTokenEntity;
import com.ctuconnect.entity.UserEntity;
import com.ctuconnect.exception.EmailAlreadyExistsException;
import com.ctuconnect.exception.UsernameAlreadyExistsException;
import com.ctuconnect.repository.EmailVerificationRepository;
import com.ctuconnect.repository.RefreshTokenRepository;
import com.ctuconnect.repository.UserRepository;
import com.ctuconnect.security.JwtService;
import com.ctuconnect.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock UserRepository userRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock EmailVerificationRepository emailVerificationRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock AuthenticationManager authenticationManager;
    @Mock EmailService emailService;
    @Mock KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    AuthServiceImpl authService;

    // ── Shared fixtures ───────────────────────────────────────────────────────

    private UserEntity activeVerifiedUser() {
        return UserEntity.builder()
                .id(UUID.randomUUID())
                .email("student@student.ctu.edu.vn")
                .username("teststudent")
                .password("$2a$10$hashed")
                .role("USER")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private RegisterRequest validRegisterRequest() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("student@student.ctu.edu.vn");
        req.setUsername("teststudent");
        req.setPassword("Password1!");
        return req;
    }

    // ── register() ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("register: saves user, sends verification email, publishes Kafka event, returns tokens")
    void register_happyPath() {
        RegisterRequest req = validRegisterRequest();
        UserEntity saved = activeVerifiedUser();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hashed");
        when(userRepository.save(any())).thenReturn(saved);
        when(emailVerificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(jwtService.generateToken(any())).thenReturn("access-token");
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(null);

        AuthResponse response = authService.register(req);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");

        verify(emailService).sendVerificationEmail(eq(saved.getEmail()), anyString());
        verify(kafkaTemplate).send(eq("user-registration"), anyString(), any());
    }

    @Test
    @DisplayName("register: normalises email to lowercase before saving")
    void register_normalisesEmailToLowercase() {
        RegisterRequest req = validRegisterRequest();
        req.setEmail("STUDENT@student.ctu.edu.vn");

        when(userRepository.existsByEmail("student@student.ctu.edu.vn")).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(i -> {
            UserEntity u = i.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });
        when(emailVerificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(jwtService.generateToken(any())).thenReturn("tok");
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(null);

        authService.register(req);

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getEmail()).isEqualTo("student@student.ctu.edu.vn");
    }

    @Test
    @DisplayName("register: throws EmailAlreadyExistsException when email is taken")
    void register_duplicateEmail_throws() {
        RegisterRequest req = validRegisterRequest();
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register: throws UsernameAlreadyExistsException when username is taken")
    void register_duplicateUsername_throws() {
        RegisterRequest req = validRegisterRequest();
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(UsernameAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register: assigns STUDENT role for @student.ctu.edu.vn email in Kafka event")
    void register_studentEmail_publishesStudentRole() {
        RegisterRequest req = validRegisterRequest(); // student email
        UserEntity saved = activeVerifiedUser();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any())).thenReturn(saved);
        when(emailVerificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(jwtService.generateToken(any())).thenReturn("tok");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.Map<String, Object>> eventCaptor =
                ArgumentCaptor.forClass(java.util.Map.class);
        when(kafkaTemplate.send(anyString(), anyString(), eventCaptor.capture())).thenReturn(null);

        authService.register(req);

        // The user-registration event should carry role=STUDENT
        java.util.Map<String, Object> event = eventCaptor.getValue();
        assertThat(event.get("role")).isEqualTo("STUDENT");
    }

    // ── login() ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("login: returns tokens for a verified, active user")
    void login_happyPath() {
        UserEntity user = activeVerifiedUser();
        EmailVerificationEntity verification = EmailVerificationEntity.builder()
                .isVerified(true).build();

        LoginRequest req = new LoginRequest();
        req.setIdentifier("student@student.ctu.edu.vn");
        req.setPassword("Password1!");

        when(userRepository.findByEmailOrUsername("student@student.ctu.edu.vn"))
                .thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword()));
        when(emailVerificationRepository.findByUser(user))
                .thenReturn(Optional.of(verification));
        when(jwtService.generateToken(any())).thenReturn("access-token");
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AuthResponse response = authService.login(req);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("login: throws RuntimeException when user is not found")
    void login_userNotFound_throws() {
        LoginRequest req = new LoginRequest();
        req.setIdentifier("nobody@student.ctu.edu.vn");
        req.setPassword("Password1!");

        when(userRepository.findByEmailOrUsername(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("login: throws RuntimeException when email is not verified")
    void login_emailNotVerified_throws() {
        UserEntity user = activeVerifiedUser();
        EmailVerificationEntity unverified = EmailVerificationEntity.builder()
                .isVerified(false).build();

        LoginRequest req = new LoginRequest();
        req.setIdentifier("student@student.ctu.edu.vn");
        req.setPassword("Password1!");

        when(userRepository.findByEmailOrUsername(anyString())).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword()));
        when(emailVerificationRepository.findByUser(user))
                .thenReturn(Optional.of(unverified));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Email not verified");
    }

    @Test
    @DisplayName("login: propagates BadCredentialsException from AuthenticationManager")
    void login_wrongPassword_throws() {
        UserEntity user = activeVerifiedUser();

        LoginRequest req = new LoginRequest();
        req.setIdentifier("student@student.ctu.edu.vn");
        req.setPassword("WrongPass1!");

        when(userRepository.findByEmailOrUsername(anyString())).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(BadCredentialsException.class);
    }

    // ── determineUserServiceRole (via register) ───────────────────────────────

    @Test
    @DisplayName("register: assigns LECTURER role for @ctu.edu.vn email in Kafka event")
    void register_lecturerEmail_publishesLecturerRole() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("lecturer@ctu.edu.vn");
        req.setUsername("lecturer1");
        req.setPassword("Password1!");

        UserEntity saved = UserEntity.builder()
                .id(UUID.randomUUID()).email("lecturer@ctu.edu.vn").username("lecturer1")
                .password("hashed").role("USER").isActive(true)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any())).thenReturn(saved);
        when(emailVerificationRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(refreshTokenRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(jwtService.generateToken(any())).thenReturn("tok");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.Map<String, Object>> eventCaptor =
                ArgumentCaptor.forClass(java.util.Map.class);
        when(kafkaTemplate.send(anyString(), anyString(), eventCaptor.capture())).thenReturn(null);

        authService.register(req);

        assertThat(eventCaptor.getValue().get("role")).isEqualTo("LECTURER");
    }
}
