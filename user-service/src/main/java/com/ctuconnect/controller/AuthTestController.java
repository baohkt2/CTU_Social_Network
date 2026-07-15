package com.ctuconnect.controller;

import com.ctuconnect.dto.AuthContextDTO;
import com.ctuconnect.security.SecurityContextHolder;
import com.ctuconnect.security.annotation.RequireAuth;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller để test và demo các chức năng xác thực (chỉ bật với profile {@code dev}).
 */
@RestController
@RequestMapping("/api/auth-test")
@Profile("dev")
public class AuthTestController {

    /**
     * Endpoint public - không cần xác thực
     */
    @GetMapping("/public")
    public ResponseEntity<String> publicEndpoint() {
        return ResponseEntity.ok("This is a public endpoint - no authentication required");
    }

    /**
     * Endpoint cần xác thực cơ bản
     */
    @GetMapping("/protected")
    @RequireAuth
    public ResponseEntity<AuthContextDTO> protectedEndpoint() {
        AuthContextDTO context = AuthContextDTO.fromAuthenticatedUser(
            SecurityContextHolder.getAuthenticatedUser()
        );
        return ResponseEntity.ok(context);
    }

    /**
     * Endpoint chỉ dành cho ADMIN
     */
    @GetMapping("/admin-only")
    @RequireAuth(roles = {"ADMIN"})
    public ResponseEntity<String> adminOnlyEndpoint() {
        String currentUserId = SecurityContextHolder.getCurrentUserId();
        return ResponseEntity.ok("Hello Admin! Current user ID: " + currentUserId);
    }

    /**
     * Endpoint user chỉ có thể truy cập dữ liệu của chính mình
     */
    @GetMapping("/user/{userId}/data")
    @RequireAuth(selfOnly = true)
    public ResponseEntity<String> userDataEndpoint(@PathVariable String userId) {
        String currentUserId = SecurityContextHolder.getCurrentUserId();
        return ResponseEntity.ok("Accessing data for user: " + userId +
                                ". Current authenticated user: " + currentUserId);
    }

    /**
     * Endpoint chỉ ADMIN hoặc USER role mới truy cập được
     */
    @GetMapping("/user-or-admin")
    @RequireAuth(roles = {"ADMIN", "USER"})
    public ResponseEntity<String> userOrAdminEndpoint() {
        String role = SecurityContextHolder.getCurrentUserRole();
        return ResponseEntity.ok("Hello " + role + "! You have access to this endpoint.");
    }

    /**
     * Endpoint cho SYSTEM role (dành cho inter-service communication)
     */
    @PostMapping("/system-sync")
    @RequireAuth(roles = {"SYSTEM"})
    public ResponseEntity<String> systemSyncEndpoint(@RequestBody String data) {
        return ResponseEntity.ok("System sync completed. Data received: " + data);
    }
}
