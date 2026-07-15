package com.ctuconnect.service;

import com.ctuconnect.dto.UserDTO;
import com.ctuconnect.dto.AuthorDTO;
import com.ctuconnect.entity.UserEntity;
import com.ctuconnect.repository.UserRepository;
import com.ctuconnect.security.SecurityContextHolder;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;

@Service
@Slf4j
public class UserSyncService {

    @Autowired
    private UserRepository userRepository;

    /**
     * Đồng bộ user từ auth-service khi tạo mới.
     */
    @Transactional
    public UserDTO syncUserFromAuth(String userId, String email, String role) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalStateException("User already exists: " + userId);
        }

        UserEntity user = new UserEntity();
        user.setId(userId);
        user.setEmail(email);
        user.setRole(role);
        user.setIsActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        return mapToDTO(userRepository.save(user));
    }

    /**
     * Cập nhật thông tin user từ auth-db
     */
    @Transactional
    public UserDTO updateUserFromAuth(String userId, String email, String role) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));

        user.setEmail(email);
        user.setRole(role);
        user.setUpdatedAt(LocalDateTime.now());

        return mapToDTO(userRepository.save(user));
    }

    /**
     * Tạo hoặc cập nhật user từ auth-service (email, username, role)
     */
    @Transactional
    public UserDTO createUserFromAuthService(String userId, String email, String username, String role) {
        if (userRepository.existsById(userId)) {
            return updateUserFromAuth(userId, email, role);
        }

        UserEntity user = UserEntity.fromAuthService(userId, email, username, role);
        return mapToDTO(userRepository.save(user));
    }

    /**
     * Xóa user và các mối quan hệ liên quan (friendship, friend requests).
     */
    @Transactional
    public void deleteUserFromAuth(String userId) {
        if (!userRepository.existsById(userId)) {
            throw new RuntimeException("User not found: " + userId);
        }

        // Xóa mối quan hệ bạn bè
        var friends = userRepository.findFriends(userId);
        for (var friend : friends) {
            userRepository.removeFriend(userId, friend.getId());
        }

        // Xóa lời mời đã gửi
        var sentRequests = userRepository.findSentFriendRequests(userId);
        for (var req : sentRequests) {
            userRepository.rejectFriendRequest(userId, req.getId());
        }

        // Xóa lời mời đã nhận
        var receivedRequests = userRepository.findReceivedFriendRequests(userId);
        for (var req : receivedRequests) {
            userRepository.rejectFriendRequest(req.getId(), userId);
        }

        userRepository.deleteById(userId);
    }

    /**
     * Kiểm tra dữ liệu đồng bộ từ auth-db
     */
    public boolean isUserSynced(String userId, String email, String role) {
        return userRepository.findById(userId)
                .map(user -> email.equals(user.getEmail()) && role.equals(user.getRole()))
                .orElse(false);
    }

    /**
     * Kiểm tra quyền truy cập user
     */
    public void validateUserAccess(String userId) {
        String currentUserId = SecurityContextHolder.getCurrentUserId();
        if (!userId.equals(currentUserId)) {
            throw new SecurityException("Access denied: User can only access their own data");
        }
    }


    /**
     * Lấy thông tin tác giả cho post-service
     * Trả về null nếu không tìm thấy user (không throw exception để tránh 500 error)
     */
    public AuthorDTO getAuthorInfo(String authorId) {
        return userRepository.findById(authorId)
                .map(user -> {
                    AuthorDTO authorDTO = new AuthorDTO();
                    authorDTO.setId(user.getId());
                    authorDTO.setFullName(user.getFullName());
                    authorDTO.setAvatarUrl(user.getAvatarUrl());
                    return authorDTO;
                })
                .orElse(null);
    }

    /**
     * Mapping UserEntity sang UserDTO
     */
    private UserDTO mapToDTO(UserEntity userEntity) {
        UserDTO dto = new UserDTO();
        dto.setId(userEntity.getId());
        dto.setEmail(userEntity.getEmail());
        dto.setUsername(userEntity.getUsername());
        dto.setFullName(userEntity.getFullName());
        dto.setStudentId(userEntity.getStudentId());
        dto.setRole(userEntity.getRole());
        dto.setBio(userEntity.getBio());
        dto.setIsActive(userEntity.isActive());
        dto.setCreatedAt(userEntity.getCreatedAt());
        dto.setUpdatedAt(userEntity.getUpdatedAt());

        // Try to get profile with relationships, but don't fail if it doesn't exist
        try {
            var userWithRels = userRepository.findUserWithRelationships(userEntity.getId()).orElse(userEntity);
            if (userWithRels.getMajor() != null) {
                dto.setMajor(userWithRels.getMajor().getName());
                if (userWithRels.getMajor().getFaculty() != null) {
                    dto.setFaculty(userWithRels.getMajor().getFaculty().getName());
                    if (userWithRels.getMajor().getFaculty().getCollege() != null) {
                        dto.setCollege(userWithRels.getMajor().getFaculty().getCollege().getName());
                    }
                }
            }
            if (userWithRels.getBatch() != null && userWithRels.getBatch().getYear() != null) {
                dto.setBatch(String.valueOf(userWithRels.getBatch().getYear()));
            }
            if (userWithRels.getGender() != null) {
                dto.setGender(userWithRels.getGender().getName());
            }
        } catch (Exception e) {
            // Profile not available yet - this is OK for new users
            log.debug("Profile not available for user {}: {}", userEntity.getId(), e.getMessage());
        }

        return dto;
    }
}
