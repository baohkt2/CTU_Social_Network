package com.ctuconnect.service;

import com.ctuconnect.dto.UserProfileDTO;
import com.ctuconnect.dto.UserUpdateDTO;
import com.ctuconnect.dto.UserSearchDTO;
import com.ctuconnect.dto.FriendRequestDTO;
import com.ctuconnect.dto.UserAcademicProfileDTO;
import com.ctuconnect.dto.FriendCandidateResponseDTO;
import com.ctuconnect.entity.UserEntity;
import com.ctuconnect.exception.UserNotFoundException;
import com.ctuconnect.exception.InvalidOperationException;
import com.ctuconnect.exception.DuplicateResourceException;
import com.ctuconnect.mapper.UserMapper;
import com.ctuconnect.repository.UserRepository;
import com.ctuconnect.repository.MajorRepository;
import com.ctuconnect.repository.BatchRepository;
import com.ctuconnect.repository.GenderRepository;
import com.ctuconnect.event.UserCreatedEvent;
import com.ctuconnect.event.UserUpdatedEvent;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Validated
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final MajorRepository majorRepository;
    private final BatchRepository batchRepository;
    private final GenderRepository genderRepository;
    private final UserMapper userMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String USER_CREATED_TOPIC = "user-created";
    private static final String USER_UPDATED_TOPIC = "user-updated";

    // User Profile Management

    @Transactional(readOnly = true)
    public UserProfileDTO getUserProfile(@NotBlank String userId) {
        log.info("Fetching user profile for userId: {}", userId);

        // Use standard findById which loads relationships automatically
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
        // Log the relationship status and image URLs for debugging
        log.info("User relationships loaded - major: {}, batch: {}, gender: {}", 
                user.getMajor() != null ? user.getMajor().getName() : "null", 
                user.getBatch() != null ? user.getBatch().getYear() : "null", 
                user.getGender() != null ? user.getGender().getName() : "null");
        log.info("User image URLs - avatarUrl: {}, backgroundUrl: {}", 
                user.getAvatarUrl(), user.getBackgroundUrl());

        UserProfileDTO dto = userMapper.toUserProfileDTO(user);
        log.info("UserProfileDTO created - avatarUrl: {}, backgroundUrl: {}", 
                dto.getAvatarUrl(), dto.getBackgroundUrl());
        
        // Fallback: If avatarUrl is null in DTO, try to fetch it separately
        if (dto.getAvatarUrl() == null && user.getAvatarUrl() == null) {
            log.warn("avatarUrl is null, attempting to fetch separately for userId: {}", userId);
            String avatarUrl = userRepository.findAvatarUrlById(userId);
            if (avatarUrl != null) {
                dto.setAvatarUrl(avatarUrl);
                log.info("Successfully fetched avatarUrl via fallback: {}", avatarUrl);
            } else {
                log.warn("No avatarUrl found even after fallback query for userId: {}", userId);
            }
        }
        
        return dto;
    }

    @Transactional(readOnly = true)
    public UserProfileDTO getUserProfileByEmail(@NotBlank String email) {
        log.info("Fetching user profile for email: {}", email);

        var user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));

        return getUserProfile(user.getId());
    }

    public UserEntity createUser(@NotBlank String authUserId,
                                @NotBlank String email,
                                String username,
                                @NotBlank String role) {
        log.info("Creating user with authUserId: {}, email: {}", authUserId, email);

        // Check if user already exists
        if (userRepository.existsById(authUserId)) {
            throw new DuplicateResourceException("User already exists with ID: " + authUserId);
        }

        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("User already exists with email: " + email);
        }

        // Create user entity
        var user = UserEntity.fromAuthService(authUserId, email, username, role);
        var savedUser = userRepository.save(user);

        // Publish user created event
        publishUserCreatedEvent(savedUser);

        log.info("User created successfully with ID: {}", savedUser.getId());
        return savedUser;
    }

    public UserProfileDTO updateUserProfile(@NotBlank String userId,
                                          @Valid UserUpdateDTO updateDTO) {
        log.info("Updating user profile for userId: {}", userId);

        var user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        // Update basic profile information
        if (updateDTO.getFullName() != null) {
            user.setFullName(updateDTO.getFullName());
        }

        if (updateDTO.getBio() != null) {
            user.setBio(updateDTO.getBio());
        }

        if (updateDTO.getStudentId() != null) {
            // Check if student ID is already taken
            if (!updateDTO.getStudentId().equals(user.getStudentId()) &&
                userRepository.existsByStudentId(updateDTO.getStudentId())) {
                throw new DuplicateResourceException("Student ID already exists: " + updateDTO.getStudentId());
            }
            user.setStudentId(updateDTO.getStudentId());
        }

        // Update avatar and background images
        if (updateDTO.getAvatarUrl() != null) {
            user.setAvatarUrl(updateDTO.getAvatarUrl());
            log.info("Updated avatar URL for userId: {}, avatarUrl: {}", userId, updateDTO.getAvatarUrl());
        }

        if (updateDTO.getBackgroundUrl() != null) {
            user.setBackgroundUrl(updateDTO.getBackgroundUrl());
            log.info("Updated background URL for userId: {}", userId);
        }

        user.setUpdatedAt(LocalDateTime.now());
        // Save immediately to persist image URLs
        user = userRepository.save(user);

        // Update academic information using custom queries that properly handle relationships
        if (updateDTO.getMajorCode() != null && !updateDTO.getMajorCode().isEmpty()) {
            
            var major = majorRepository.findByCode(updateDTO.getMajorCode())
                .or(() -> majorRepository.findByName(updateDTO.getMajorCode()))
                .orElseThrow(() -> new UserNotFoundException("Major not found with code/name: " + updateDTO.getMajorCode()));
                
            // Update relationship using the actual name of the major in the database
            userRepository.updateUserMajor(userId, major.getName());
            log.info("Updated major relationship for userId: {} to major: {}", userId, major.getName());
        }

        if (updateDTO.getBatchYear() != null && !updateDTO.getBatchYear().isEmpty()) {
            Integer batchYearInt = Integer.valueOf(updateDTO.getBatchYear());
            batchRepository.findByYear(batchYearInt)
                .orElseThrow(() -> new UserNotFoundException("Batch not found: " + updateDTO.getBatchYear()));
            // Update relationship
            userRepository.updateUserBatch(userId, batchYearInt);
            log.info("Updated batch relationship for userId: {} to batchYear: {}", userId, updateDTO.getBatchYear());
        }

        if (updateDTO.getGenderName() != null && !updateDTO.getGenderName().isEmpty()) {
            // Try to find gender by code first, then by name
            var gender = genderRepository.findByCode(updateDTO.getGenderName())
                .or(() -> genderRepository.findByName(updateDTO.getGenderName()))
                .orElseThrow(() -> new UserNotFoundException("Gender not found: " + updateDTO.getGenderName()));
            // Update relationship using gender name
            userRepository.updateUserGender(userId, gender.getName());
            log.info("Updated gender relationship for userId: {} to genderName: {}", userId, gender.getName());
        }

        // Fetch updated user with relationships
        var updatedUser = userRepository.findUserWithRelationships(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        // Publish user updated event
        publishUserUpdatedEvent(updatedUser);

        log.info("User profile updated successfully for userId: {}", userId);
        return userMapper.toUserProfileDTO(updatedUser);
    }

    public void deactivateUser(@NotBlank String userId) {
        log.info("Deactivating user with userId: {}", userId);

        var user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        user.deactivate();
        userRepository.save(user);

        publishUserUpdatedEvent(user);

        log.info("User deactivated successfully for userId: {}", userId);
    }

    public void activateUser(@NotBlank String userId) {
        log.info("Activating user with userId: {}", userId);

        var user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        user.activate();
        userRepository.save(user);

        publishUserUpdatedEvent(user);

        log.info("User activated successfully for userId: {}", userId);
    }

    // User Search and Discovery

    @Transactional(readOnly = true)
    public Page<UserSearchDTO> searchUsers(@NotBlank String searchTerm,
                                         String currentUserId,
                                         @NotNull Pageable pageable) {
        log.info("Searching users with term: {}, currentUserId: {}", searchTerm, currentUserId);

        var searchResults = userRepository.searchUsers(searchTerm, currentUserId);
        
        // Apply pagination manually
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), searchResults.size());
        
        if (start >= searchResults.size()) {
            return new org.springframework.data.domain.PageImpl<>(new ArrayList<>(), pageable, searchResults.size());
        }
        
        List<UserSearchDTO> dtos = searchResults.subList(start, end).stream()
            .map(userMapper::toUserSearchDTO)
            .collect(Collectors.toList());
            
        return new org.springframework.data.domain.PageImpl<>(dtos, pageable, searchResults.size());
    }

    @Transactional(readOnly = true)
    public Page<UserSearchDTO> findUsersByCollege(@NotBlank String collegeName,
                                                String currentUserId,
                                                @NotNull Pageable pageable) {
        log.info("Finding users by college: {}, currentUserId: {}", collegeName, currentUserId);

        var searchResults = userRepository.findUsersByCollege(collegeName, currentUserId);
        
        // Apply pagination manually
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), searchResults.size());
        
        if (start >= searchResults.size()) {
            return new org.springframework.data.domain.PageImpl<>(new ArrayList<>(), pageable, searchResults.size());
        }
        
        List<UserSearchDTO> dtos = searchResults.subList(start, end).stream()
            .map(userMapper::toUserSearchDTO)
            .collect(Collectors.toList());
            
        return new org.springframework.data.domain.PageImpl<>(dtos, pageable, searchResults.size());
    }

    @Transactional(readOnly = true)
    public Page<UserSearchDTO> findUsersByFaculty(@NotBlank String facultyName,
                                                String currentUserId,
                                                @NotNull Pageable pageable) {
        log.info("Finding users by faculty: {}, currentUserId: {}", facultyName, currentUserId);

        var searchResults = userRepository.findUsersByFaculty(facultyName, currentUserId);
        
        // Apply pagination manually
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), searchResults.size());
        
        if (start >= searchResults.size()) {
            return new org.springframework.data.domain.PageImpl<>(new ArrayList<>(), pageable, searchResults.size());
        }
        
        List<UserSearchDTO> dtos = searchResults.subList(start, end).stream()
            .map(userMapper::toUserSearchDTO)
            .collect(Collectors.toList());
            
        return new org.springframework.data.domain.PageImpl<>(dtos, pageable, searchResults.size());
    }

    @Transactional(readOnly = true)
    public Page<UserSearchDTO> findUsersByMajor(@NotBlank String majorName,
                                              String currentUserId,
                                              @NotNull Pageable pageable) {
        log.info("Finding users by major: {}, currentUserId: {}", majorName, currentUserId);

        var searchResults = userRepository.findUsersByMajor(majorName, currentUserId);
        
        // Apply pagination manually
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), searchResults.size());
        
        if (start >= searchResults.size()) {
            return new org.springframework.data.domain.PageImpl<>(new ArrayList<>(), pageable, searchResults.size());
        }
        
        List<UserSearchDTO> dtos = searchResults.subList(start, end).stream()
            .map(userMapper::toUserSearchDTO)
            .collect(Collectors.toList());
            
        return new org.springframework.data.domain.PageImpl<>(dtos, pageable, searchResults.size());
    }

    @Transactional(readOnly = true)
    public Page<UserSearchDTO> findUsersByBatch(@NotNull String batchYear,
                                              String currentUserId,
                                              @NotNull Pageable pageable) {
        log.info("Finding users by batch: {}, currentUserId: {}", batchYear, currentUserId);

        var searchResults = userRepository.findUsersByBatch(batchYear, currentUserId);
        
        // Apply pagination manually
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), searchResults.size());
        
        if (start >= searchResults.size()) {
            return new org.springframework.data.domain.PageImpl<>(new ArrayList<>(), pageable, searchResults.size());
        }
        
        List<UserSearchDTO> dtos = searchResults.subList(start, end).stream()
            .map(userMapper::toUserSearchDTO)
            .collect(Collectors.toList());
            
        return new org.springframework.data.domain.PageImpl<>(dtos, pageable, searchResults.size());
    }

    // Friend Management

    @Transactional(readOnly = true)
    public Page<UserSearchDTO> getFriends(@NotBlank String userId, @NotNull Pageable pageable) {
        log.info("Getting friends for userId: {}", userId);
        
        try {
            log.debug("Step 1: Calling userRepository.findFriends");
            
            var friends = userRepository.findFriends(userId);
            
            log.debug("Step 2: Retrieved {} friends from repository", friends.size());
            log.debug("Step 3: Starting DTO mapping for {} friends", friends.size());
            
            // Apply pagination manually
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), friends.size());
            
            List<UserSearchDTO> friendDTOs = friends.subList(start, end).stream()
                .map(user -> {
                    try {
                        log.trace("Mapping friend: {}", user.getId());
                        return userMapper.toUserSearchDTO(user);
                    } catch (Exception e) {
                        log.error("Error mapping friend to DTO: {}", e.getMessage(), e);
                        throw new RuntimeException("Error mapping friend data", e);
                    }
                })
                .collect(Collectors.toList());
            
            var result = new org.springframework.data.domain.PageImpl<>(
                friendDTOs, pageable, friends.size());
            
            log.debug("Step 4: Successfully mapped all friends to DTOs");
            log.info("Successfully retrieved {} friends for userId: {}", result.getTotalElements(), userId);
            
            return result;
        } catch (Exception e) {
            log.error("Error getting friends for userId {}: {}", userId, e.getMessage(), e);
            throw new RuntimeException("Failed to get friends list", e);
        }
    }

    @Transactional(readOnly = true)
    public List<FriendRequestDTO> getSentFriendRequests(@NotBlank String userId) {
        log.info("Getting sent friend requests for userId: {}", userId);

        var sentRequests = userRepository.findSentFriendRequests(userId);
        return sentRequests.stream()
            .map(user -> userMapper.toFriendRequestDTO(user, "SENT"))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<FriendRequestDTO> getReceivedFriendRequests(@NotBlank String userId) {
        log.info("Getting received friend requests for userId: {}", userId);

        var receivedRequests = userRepository.findReceivedFriendRequests(userId);
        return receivedRequests.stream()
            .map(user -> userMapper.toFriendRequestDTO(user, "RECEIVED"))
            .collect(Collectors.toList());
    }

    /**
     * Get all friend requests (both sent and received) for a user
     * This combines both sent and received requests in a single list
     */
    @Transactional(readOnly = true)
    public List<FriendRequestDTO> getAllFriendRequests(@NotBlank String userId) {
        log.info("Getting all friend requests (sent + received) for userId: {}", userId);

        List<FriendRequestDTO> allRequests = new ArrayList<>();
        
        // Get sent requests
        var sentRequests = userRepository.findSentFriendRequests(userId);
        allRequests.addAll(sentRequests.stream()
            .map(user -> userMapper.toFriendRequestDTO(user, "SENT"))
            .collect(Collectors.toList()));
        
        // Get received requests
        var receivedRequests = userRepository.findReceivedFriendRequests(userId);
        allRequests.addAll(receivedRequests.stream()
            .map(user -> userMapper.toFriendRequestDTO(user, "RECEIVED"))
            .collect(Collectors.toList()));
        
        log.info("Found {} total friend requests ({} sent, {} received)", 
                allRequests.size(), sentRequests.size(), receivedRequests.size());
        
        return allRequests;
    }

    public void sendFriendRequest(@NotBlank String senderId, @NotBlank String receiverId) {
        log.info("Sending friend request from userId: {} to userId: {}", senderId, receiverId);

        if (senderId.equals(receiverId)) {
            throw new InvalidOperationException("Cannot send friend request to yourself");
        }

        // Verify both users exist and are active
        var sender = userRepository.findById(senderId)
            .orElseThrow(() -> new UserNotFoundException("Sender not found with ID: " + senderId));

        var receiver = userRepository.findById(receiverId)
            .orElseThrow(() -> new UserNotFoundException("Receiver not found with ID: " + receiverId));

        if (!sender.isActive() || !receiver.isActive()) {
            throw new InvalidOperationException("Both users must be active to send friend request");
        }

        boolean success = userRepository.sendFriendRequest(senderId, receiverId);

        if (!success) {
            throw new InvalidOperationException("Unable to send friend request. Users may already be friends or request already exists");
        }

        log.info("Friend request sent successfully from userId: {} to userId: {}", senderId, receiverId);
    }

    public void acceptFriendRequest(@NotBlank String requesterId, @NotBlank String accepterId) {
        log.info("Accepting friend request from userId: {} by userId: {}", requesterId, accepterId);

        boolean success = userRepository.acceptFriendRequest(requesterId, accepterId);

        if (!success) {
            throw new InvalidOperationException("Unable to accept friend request. Request may not exist or users may be inactive");
        }

        log.info("Friend request accepted successfully from userId: {} by userId: {}", requesterId, accepterId);
    }

    /**
     * Reject/Cancel friend request - supports both directions
     * - If user1 sent request to user2: user2 can reject OR user1 can cancel
     * - Works regardless of who calls it
     */
    public void rejectFriendRequest(@NotBlank String userId1, @NotBlank String userId2) {
        log.info("Rejecting/Canceling friend request between userId: {} and userId: {}", userId1, userId2);

        boolean success = userRepository.rejectFriendRequest(userId1, userId2);

        if (!success) {
            throw new InvalidOperationException("Unable to reject/cancel friend request. Request may not exist");
        }

        log.info("Friend request rejected/cancelled successfully between userId: {} and userId: {}", userId1, userId2);
    }

    public void removeFriend(@NotBlank String userId1, @NotBlank String userId2) {
        log.info("Removing friendship between userId: {} and userId: {}", userId1, userId2);

        boolean success = userRepository.removeFriend(userId1, userId2);

        if (!success) {
            throw new InvalidOperationException("Unable to remove friendship. Users may not be friends");
        }

        log.info("Friendship removed successfully between userId: {} and userId: {}", userId1, userId2);
    }

    // Utility Methods

    @Transactional(readOnly = true)
    public boolean userExists(@NotBlank String userId) {
        return userRepository.existsById(userId);
    }

    @Transactional(readOnly = true)
    public boolean emailExists(@NotBlank String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional(readOnly = true)
    public boolean studentIdExists(@NotBlank String studentId) {
        return userRepository.existsByStudentId(studentId);
    }

    @Transactional(readOnly = true)
    public List<UserEntity> getAllActiveUsers() {
        return userRepository.findByIsActiveTrue();
    }

    // Event Publishing

    private void publishUserCreatedEvent(UserEntity user) {
        try {
            var event = UserCreatedEvent.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();

            kafkaTemplate.send(USER_CREATED_TOPIC, user.getId(), event);
            log.info("Published user created event for userId: {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to publish user created event for userId: {}", user.getId(), e);
        }
    }

    private void publishUserUpdatedEvent(UserEntity user) {
        try {
            var event = UserUpdatedEvent.builder()
                .userId(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .bio(user.getBio())
                .studentId(user.getStudentId())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .updatedAt(user.getUpdatedAt())
                .build();

            kafkaTemplate.send(USER_UPDATED_TOPIC, user.getId(), event);
            log.info("Published user updated event for userId: {}", user.getId());
        } catch (Exception e) {
            log.error("Failed to publish user updated event for userId: {}", user.getId(), e);
        }
    }

    // Methods for post-service integration

    @Transactional(readOnly = true)
    public java.util.Set<String> getFriendIds(@NotBlank String userId) {
        log.info("Getting friend IDs for userId: {}", userId);
        var user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
        
        return user.getFriends().stream()
            .map(UserEntity::getId)
            .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public java.util.Set<String> getCloseInteractionIds(@NotBlank String userId) {
        log.info("Getting close interaction IDs for userId: {}", userId);
        // Return friend IDs as close interactions for now
        // This can be enhanced with actual interaction tracking
        return getFriendIds(userId);
    }

    @Transactional(readOnly = true)
    public java.util.Set<String> getSameFacultyUserIds(@NotBlank String userId) {
        log.info("Getting same faculty user IDs for userId: {}", userId);
        UserEntity user = null;
        try {
            user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
        } catch (Exception e) {
            log.error("Error finding user by ID: ", e);
            throw e;
        }
        
        if (user.getMajor() == null || user.getMajor().getFaculty() == null) {
            return new java.util.HashSet<>();
        }
        
        String facultyName = user.getMajor().getFaculty().getName();
        var users = userRepository.findUsersByFaculty(facultyName, userId);
        
        return users.stream()
            .map(UserEntity::getId)
            .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public java.util.Set<String> getSameMajorUserIds(@NotBlank String userId) {
        log.info("Getting same major user IDs for userId: {}", userId);
        var user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
        
        if (user.getMajor() == null) {
            return new java.util.HashSet<>();
        }
        
        String majorName = user.getMajor().getName();
        var users = userRepository.findUsersByMajor(majorName, userId);
        
        return users.stream()
            .map(UserEntity::getId)
            .collect(Collectors.toSet());
    }

    @Transactional(readOnly = true)
    public java.util.Set<String> getUserInterestTags(@NotBlank String userId) {
        log.info("Getting interest tags for userId: {}", userId);
        // Return empty set for now
        // This can be enhanced with user preferences/interests tracking
        return new java.util.HashSet<>();
    }

    @Transactional(readOnly = true)
    public java.util.Set<String> getUserPreferredCategories(@NotBlank String userId) {
        log.info("Getting preferred categories for userId: {}", userId);
        // Return empty set for now
        // This can be enhanced with user category preferences tracking
        return new java.util.HashSet<>();
    }

    @Transactional(readOnly = true)
    public String getUserFacultyId(@NotBlank String userId) {
        log.info("Getting faculty ID for userId: {}", userId);
        var user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
        
        if (user.getMajor() != null && user.getMajor().getFaculty() != null) {
            return user.getMajor().getFaculty().getCode();
        }
        return null;
    }

    @Transactional(readOnly = true)
    public String getUserMajorId(@NotBlank String userId) {
        log.info("Getting major ID for userId: {}", userId);
        var user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
        
        if (user.getMajor() != null) {
            return user.getMajor().getName();
        }
        return null;
    }

    @Transactional(readOnly = true)
    public List<com.ctuconnect.dto.UserDTO> searchUsersWithContext(
            @NotBlank String query,
            String faculty,
            String major,
            String batch,
            String currentUserId,
            @Min(0) int page,
            @Min(1) @Max(100) int size) {
        log.info("Searching users with context: query={}, faculty={}, major={}, batch={}", 
                 query, faculty, major, batch);
        
        List<UserEntity> results;
        
        // Search by specific filters
        if (major != null && !major.isEmpty()) {
            results = userRepository.findUsersByMajor(major, currentUserId);
        } else if (faculty != null && !faculty.isEmpty()) {
            results = userRepository.findUsersByFaculty(faculty, currentUserId);
        } else if (batch != null && !batch.isEmpty()) {
            try {
               
                results = userRepository.findUsersByBatch(batch, currentUserId);
            } catch (NumberFormatException e) {
                results = userRepository.searchUsers(query, currentUserId);
            }
        } else {
            results = userRepository.searchUsers(query, currentUserId);
        }
        
        // Apply pagination manually
        int start = page * size;
        int end = Math.min((start + size), results.size());
        
        if (start >= results.size()) {
            return new ArrayList<>();
        }
        
        return results.subList(start, end).stream()
            .map(userMapper::toUserDTO)
            .collect(Collectors.toList());
    }

    public void addFriend(@NotBlank String userId, @NotBlank String targetUserId) {
        log.info("Adding friend: userId={}, targetUserId={}", userId, targetUserId);
        sendFriendRequest(userId, targetUserId);
    }

    public void acceptFriendInvite(@NotBlank String requesterId, @NotBlank String accepterId) {
        log.info("Accepting friend invite: requesterId={}, accepterId={}", requesterId, accepterId);
        acceptFriendRequest(requesterId, accepterId);
    }

    @Transactional(readOnly = true)
    public List<com.ctuconnect.dto.ActivityDTO> getUserActivity(
            @NotBlank String userId,
            String viewerId,
            @Min(0) int page,
            @Min(1) @Max(100) int size) {
        log.info("Getting user activity: userId={}, viewerId={}, page={}, size={}", 
                 userId, viewerId, page, size);
        
        // Verify user exists
        var user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
        
        // Return empty list for now
        // This can be enhanced with actual activity tracking from other services
        return new java.util.ArrayList<>();
    }

    // ==================== FRIENDSHIP STATUS ====================

    /**
     * Get friendship status between current user and target user
     * Returns: "none", "friends", "sent", "received", "self"
     */
    @Transactional(readOnly = true)
    public String getFriendshipStatus(@NotBlank String currentUserId, @NotBlank String targetUserId) {
        log.info("Getting friendship status: currentUserId={}, targetUserId={}", currentUserId, targetUserId);
        
        // Check if viewing own profile
        if (currentUserId.equals(targetUserId)) {
            return "self";
        }
        
        // Verify both users exist
        if (!userRepository.existsById(currentUserId) || !userRepository.existsById(targetUserId)) {
            throw new UserNotFoundException("One or both users not found");
        }
        
        // Check if they are friends
        if (userRepository.areFriends(currentUserId, targetUserId)) {
            return "friends";
        }
        
        // Check if current user sent request to target
        if (userRepository.hasPendingFriendRequest(currentUserId, targetUserId)) {
            return "sent";
        }
        
        // Check if target user sent request to current user
        if (userRepository.hasPendingFriendRequest(targetUserId, currentUserId)) {
            return "received";
        }
        
        return "none";
    }

    // ==================== MUTUAL FRIENDS ====================

    /**
     * Get mutual friends list between two users (paginated)
     */
    @Transactional(readOnly = true)
    public Page<UserSearchDTO> getMutualFriendsList(
            @NotBlank String userId1,
            @NotBlank String userId2,
            @NotNull Pageable pageable) {
        log.info("Getting mutual friends list: userId1={}, userId2={}", userId1, userId2);
        
        // Verify both users exist
        if (!userRepository.existsById(userId1) || !userRepository.existsById(userId2)) {
            throw new UserNotFoundException("One or both users not found");
        }
        
        // Get all mutual friends (not paginated from repository)
        List<UserEntity> mutualFriends = userRepository.findMutualFriends(userId1, userId2);
        
        // Convert to DTOs
        List<UserSearchDTO> mutualFriendDTOs = mutualFriends.stream()
            .map(userMapper::toUserSearchDTO)
            .collect(Collectors.toList());
        
        // Apply pagination manually
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), mutualFriendDTOs.size());
        
        List<UserSearchDTO> pageContent = start < mutualFriendDTOs.size() 
            ? mutualFriendDTOs.subList(start, end) 
            : new ArrayList<>();
        
        return new org.springframework.data.domain.PageImpl<>(
            pageContent, pageable, mutualFriendDTOs.size());
    }

    /**
     * Get mutual friends count between two users
     */
    @Transactional(readOnly = true)
    public int getMutualFriendsCount(@NotBlank String userId1, @NotBlank String userId2) {
        log.info("Getting mutual friends count: userId1={}, userId2={}", userId1, userId2);
        
        // Verify both users exist
        if (!userRepository.existsById(userId1) || !userRepository.existsById(userId2)) {
            return 0;
        }
        
        List<UserEntity> mutualFriends = userRepository.findMutualFriends(userId1, userId2);
        return mutualFriends.size();
    }

    // ==================== ENHANCED FRIEND SUGGESTIONS ====================

    /**
     * Search friend suggestions with filters
     * If query is provided: search by fullname/email and apply filters
     * If query is null: return suggestions based on filters only
     */
    @Transactional(readOnly = true)
    public List<UserSearchDTO> searchFriendSuggestions(
            @NotBlank String currentUserId,
            String query,
            String college,
            String faculty,
            String batch,
            @Min(1) @Max(100) int limit) {
        log.info("Searching friend suggestions: currentUserId={}, query={}, college={}, faculty={}, batch={}, limit={}",
                 currentUserId, query, college, faculty, batch, limit);
        
        try {
            // Verify user exists
            var currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + currentUserId));
            
            List<UserEntity> results;
            
            // Check which filters are provided
            boolean hasQuery = query != null && !query.trim().isEmpty();
            boolean hasFaculty = faculty != null && !faculty.isEmpty();
            boolean hasBatch = batch != null && !batch.isEmpty();
            boolean hasCollege = college != null && !college.isEmpty();
            
            // If ALL filters are null -> return random users
            if (!hasQuery && !hasFaculty && !hasBatch && !hasCollege) {
                log.debug("No filters provided, getting random users");
                results = userRepository.findRandomUsers(currentUserId, limit);
                log.debug("Found {} random users", results.size());
            }
            // If query provided -> search by query
            else if (hasQuery) {
                log.debug("Searching by query: {}", query);
                results = userRepository.searchUsers(query.trim(), currentUserId);
                log.debug("Found {} results", results.size());
            }
            // If faculty provided -> filter by faculty
            else if (hasFaculty) {
                log.debug("Filtering by faculty: {}", faculty);
                results = userRepository.findUsersByFaculty(faculty, currentUserId);
                log.debug("Found {} results", results.size());
            }
            // If batch provided -> filter by batch
            else if (hasBatch) {
                log.debug("Filtering by batch: {}", batch);
                try {
                  
                    results = userRepository.findUsersByBatch(batch, currentUserId);
                    log.debug("Found {} results", results.size());
                } catch (NumberFormatException e) {
                    log.warn("Invalid batch year: {}", batch);
                    results = new ArrayList<>();
                }
            }
            // If college provided -> filter by college
            else {
                log.debug("Filtering by college: {}", college);
                results = userRepository.findUsersByCollege(college, currentUserId);
                log.debug("Found {} results", results.size());
            }
            
            // Convert to DTOs with enhanced information
            List<UserSearchDTO> filtered = results.stream()
                .limit(limit)
                .map(user -> enhanceUserSearchDTO(currentUser, user))
                .collect(Collectors.toList());
            
            log.info("Successfully found {} friend suggestions", filtered.size());
            return filtered;
            
        } catch (Exception e) {
            log.error("Error searching friend suggestions: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to search friend suggestions", e);
        }
    }

    /**
     * Enhance UserSearchDTO with mutual friends count and relationship flags
     */
    private UserSearchDTO enhanceUserSearchDTO(UserEntity currentUser, UserEntity targetUser) {
        UserSearchDTO dto = userMapper.toUserSearchDTO(targetUser);
        
        // Calculate mutual friends count
        try {
            List<UserEntity> mutualFriends = userRepository.findMutualFriends(currentUser.getId(), targetUser.getId());
            dto.setMutualFriendsCount((long) mutualFriends.size());
        } catch (Exception e) {
            log.debug("Error calculating mutual friends: {}", e.getMessage());
            dto.setMutualFriendsCount(0L);
        }
        
        // Calculate relationship flags
        dto.setSameCollege(isSameCollege(currentUser, targetUser));
        dto.setSameFaculty(isSameFaculty(currentUser, targetUser));
        dto.setSameMajor(isSameMajor(currentUser, targetUser));
        dto.setSameBatch(isSameBatch(currentUser, targetUser));
        
        // Check friendship status
        dto.setIsFriend(currentUser.getFriends().stream()
            .anyMatch(f -> f.getId().equals(targetUser.getId())));
        dto.setRequestSent(currentUser.getSentFriendRequests().stream()
            .anyMatch(f -> f.getId().equals(targetUser.getId())));
        dto.setRequestReceived(currentUser.getReceivedFriendRequests().stream()
            .anyMatch(f -> f.getId().equals(targetUser.getId())));
        
        return dto;
    }

    private boolean isSameCollege(UserEntity u1, UserEntity u2) {
        if (u1.getMajor() == null || u2.getMajor() == null) return false;
        if (u1.getMajor().getFaculty() == null || u2.getMajor().getFaculty() == null) return false;
        if (u1.getMajor().getFaculty().getCollege() == null || u2.getMajor().getFaculty().getCollege() == null) return false;
        String c1 = u1.getMajor().getFaculty().getCollege().getName();
        String c2 = u2.getMajor().getFaculty().getCollege().getName();
        return c1 != null && c1.equals(c2);
    }

    private boolean isSameFaculty(UserEntity u1, UserEntity u2) {
        if (u1.getMajor() == null || u2.getMajor() == null) return false;
        if (u1.getMajor().getFaculty() == null || u2.getMajor().getFaculty() == null) return false;
        String f1 = u1.getMajor().getFaculty().getName();
        String f2 = u2.getMajor().getFaculty().getName();
        return f1 != null && f1.equals(f2);
    }

    private boolean isSameMajor(UserEntity u1, UserEntity u2) {
        if (u1.getMajor() == null || u2.getMajor() == null) return false;
        String m1 = u1.getMajor().getName();
        String m2 = u2.getMajor().getName();
        return m1 != null && m1.equals(m2);
    }

    private boolean isSameBatch(UserEntity u1, UserEntity u2) {
        if (u1.getBatch() == null || u2.getBatch() == null) return false;
        Integer b1 = u1.getBatch().getYear();
        Integer b2 = u2.getBatch().getYear();
        return b1 != null && b1.equals(b2);
    }

    // ===== Friend Recommendation Support Methods =====

    /**
     * Get academic profile for recommend-service
     */
    @Transactional(readOnly = true)
    public UserAcademicProfileDTO getAcademicProfile(@NotBlank String userId) {
        log.info("Getting academic profile for user: {}", userId);
        
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
        
        return UserAcademicProfileDTO.builder()
            .userId(user.getId())
            .major(user.getMajorName())
            .faculty(user.getFacultyName())
            .degree(null) // UserEntity doesn't have degree field
            .batch(user.getBatchYear() != null ? user.getBatchYear().toString() : null)
            .studentId(user.getStudentId())
            .build();
    }

    /**
     * Get friend candidates for ML recommendation
     */
    @Transactional(readOnly = true)
    public List<FriendCandidateResponseDTO> getFriendCandidates(
            @NotBlank String userId, 
            @Min(1) @Max(500) int limit) {
        
        log.info("Getting {} friend candidates for user: {}", limit, userId);
        
        UserEntity currentUser = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
        
        // Get all users except current user and existing friends
        List<UserEntity> allUsers = userRepository.findAll();
        List<UserEntity> friends = userRepository.findFriends(userId);
        
        return allUsers.stream()
            .filter(user -> !user.getId().equals(userId))
            .filter(user -> friends.stream().noneMatch(f -> f.getId().equals(user.getId())))
            .filter(user -> !userRepository.hasPendingFriendRequest(userId, user.getId()))
            .limit(limit)
            .map(candidate -> buildFriendCandidate(currentUser, candidate))
            .collect(Collectors.toList());
    }

    /**
     * Build friend candidate DTO
     */
    private FriendCandidateResponseDTO buildFriendCandidate(UserEntity currentUser, UserEntity candidate) {
        int mutualCount = getMutualFriendsCount(currentUser.getId(), candidate.getId());
        
        boolean sameFaculty = currentUser.getFacultyName() != null && 
                              currentUser.getFacultyName().equals(candidate.getFacultyName());
        boolean sameMajor = currentUser.getMajorName() != null && 
                            currentUser.getMajorName().equals(candidate.getMajorName());
        boolean sameBatch = currentUser.getBatchYear() != null && 
                            currentUser.getBatchYear().equals(candidate.getBatchYear());
        
        return FriendCandidateResponseDTO.builder()
            .userId(candidate.getId())
            .username(candidate.getUsername())
            .fullName(candidate.getFullName())
            .avatarUrl(candidate.getAvatarUrl())
            .bio(candidate.getBio())
            .facultyName(candidate.getFacultyName())
            .majorName(candidate.getMajorName())
            .batchYear(candidate.getBatchYear())
            .sameFaculty(sameFaculty)
            .sameMajor(sameMajor)
            .sameBatch(sameBatch)
            .mutualFriendsCount(mutualCount)
            .activityScore(0.5) // Default activity score
            .skills(null) // TODO: Add when user profile has skills field
            .interests(null) // TODO: Add when user profile has interests field
            .courses(null) // TODO: Add when courses are implemented
            .build();
    }

    /**
     * Check if two users are friends
     */
    @Transactional(readOnly = true)
    public boolean areFriends(@NotBlank String userId1, @NotBlank String userId2) {
        try {
            return userRepository.areFriends(userId1, userId2);
        } catch (Exception e) {
            log.warn("Error checking friendship: {}", e.getMessage());
            return false;
        }
    }
}
