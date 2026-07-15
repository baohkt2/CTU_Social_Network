package com.ctuconnect.mapper;

import com.ctuconnect.dto.UserProfileDTO;
import com.ctuconnect.dto.UserSearchDTO;
import com.ctuconnect.dto.FriendRequestDTO;
import com.ctuconnect.dto.UserDTO;
import com.ctuconnect.entity.UserEntity;
import com.ctuconnect.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@Slf4j
public class UserMapper {

    public UserSearchDTO toUserSearchDTO(UserEntity user) {
        return UserSearchDTO.builder()
            .id(user.getId())
            .email(user.getEmail())
            .username(user.getUsername())
            .studentId(user.getStudentId())
            .fullName(user.getFullName())
            .role(user.getRole())
            .isActive(user.getIsActive())
            .avatarUrl(user.getAvatarUrl())
            .bio(user.getBio())
            .college(user.getMajor() != null && user.getMajor().getFaculty() != null &&
                    user.getMajor().getFaculty().getCollege() != null ?
                    user.getMajor().getFaculty().getCollege().getName() : null)
            .faculty(user.getMajor() != null && user.getMajor().getFaculty() != null ?
                    user.getMajor().getFaculty().getName() : null)
            .major(user.getMajor() != null ? user.getMajor().getName() : null)
            .batch(user.getBatch() != null ? String.valueOf(user.getBatch().getYear()) : null)
            .gender(user.getGender() != null ? user.getGender().getName() : null)
            .friendsCount((long) user.getFriends().size())
            .mutualFriendsCount(0L)
            .isFriend(false)
            .requestSent(false)
            .requestReceived(false)
            .sameCollege(false)
            .sameFaculty(false)
            .sameMajor(false)
            .sameBatch(false)
            .build();
    }

    public FriendRequestDTO toFriendRequestDTO(UserEntity user, String requestType) {
        Integer batchYear = null;
        if (user.getBatch() != null && user.getBatch().getYear() != null) {
            batchYear = user.getBatch().getYear();
        }
        
        return FriendRequestDTO.builder()
            .id(user.getId())
            .email(user.getEmail())
            .username(user.getUsername())
            .fullName(user.getFullName())
            .studentId(user.getStudentId())
            .college(user.getMajor() != null && user.getMajor().getFaculty() != null &&
                    user.getMajor().getFaculty().getCollege() != null ?
                    user.getMajor().getFaculty().getCollege().getName() : null)
            .faculty(user.getMajor() != null && user.getMajor().getFaculty() != null ?
                    user.getMajor().getFaculty().getName() : null)
            .major(user.getMajor() != null ? user.getMajor().getName() : null)
            .batch(batchYear)
            .gender(user.getGender() != null ? user.getGender().getName() : null)
            .mutualFriendsCount(0L)
            .requestType(requestType)
            .build();
    }

    public UserProfileDTO toUserProfileDTO(UserEntity user) {
        log.debug("Mapping UserEntity to UserProfileDTO - avatarUrl: {}, backgroundUrl: {}", 
                user.getAvatarUrl(), user.getBackgroundUrl());
        
        return UserProfileDTO.builder()
            .id(user.getId())
            .email(user.getEmail())
            .username(user.getUsername())
            .studentId(user.getStudentId())
            .fullName(user.getFullName())
            .bio(user.getBio())
            .role(user.getRole())
            .isActive(user.getIsActive())
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .avatarUrl(user.getAvatarUrl())
            .backgroundUrl(user.getBackgroundUrl())
            .college(user.getMajor() != null && user.getMajor().getFaculty() != null &&
                    user.getMajor().getFaculty().getCollege() != null ?
                    user.getMajor().getFaculty().getCollege().getName() : null)
            .faculty(user.getMajor() != null && user.getMajor().getFaculty() != null ?
                    user.getMajor().getFaculty().getName() : null)
            .major(user.getMajor() != null ? user.getMajor().getName() : null)
            .batch(user.getBatch() != null ? String.valueOf(user.getBatch().getYear()) : null)
            .gender(user.getGender() != null ? user.getGender().getName() : null)
            .collegeCode(user.getMajor() != null && user.getMajor().getFaculty() != null &&
                    user.getMajor().getFaculty().getCollege() != null ?
                    user.getMajor().getFaculty().getCollege().getCode() : null)
            .facultyCode(user.getMajor() != null && user.getMajor().getFaculty() != null ?
                    user.getMajor().getFaculty().getCode() : null)
            .majorCode(user.getMajor() != null ? user.getMajor().getCode() : null)
            .batchCode(user.getBatch() != null ? String.valueOf(user.getBatch().getYear()) : null)
            .genderCode(user.getGender() != null ? user.getGender().getCode() : null)
            .friendsCount((long) user.getFriends().size())
            .sentRequestsCount((long) user.getSentFriendRequests().size())
            .receivedRequestsCount((long) user.getReceivedFriendRequests().size())
            .build();
    }

    public UserDTO toUserDTO(UserEntity user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setUsername(user.getUsername());
        dto.setStudentId(user.getStudentId());
        dto.setFullName(user.getFullName());
        dto.setRole(user.getRole());
        dto.setBio(user.getBio());
        dto.setIsActive(user.getIsActive());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        dto.setCollege(user.getMajor() != null && user.getMajor().getFaculty() != null &&
                user.getMajor().getFaculty().getCollege() != null ?
                user.getMajor().getFaculty().getCollege().getName() : null);
        dto.setFaculty(user.getMajor() != null && user.getMajor().getFaculty() != null ?
                user.getMajor().getFaculty().getName() : null);
        dto.setMajor(user.getMajor() != null ? user.getMajor().getName() : null);
        dto.setBatch(user.getBatch() != null ? String.valueOf(user.getBatch().getYear()) : null);
        dto.setGender(user.getGender() != null ? user.getGender().getName() : null);
        dto.setFriendIds(user.getFriends().stream()
            .map(UserEntity::getId)
            .collect(Collectors.toSet()));
        dto.setMutualFriendsCount(0);
        dto.setSameCollege(false);
        dto.setSameFaculty(false);
        dto.setSameMajor(false);
        
        return dto;
    }
}
