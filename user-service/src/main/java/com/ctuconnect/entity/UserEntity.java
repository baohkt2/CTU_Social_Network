package com.ctuconnect.entity;

import jakarta.validation.constraints.Email;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

import java.util.HashSet;
import java.util.Set;

@Node("User")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {
    @Id
    private String id; // UUID string from auth-service

    @Email(message = "Email format is invalid")
    @NotBlank(message = "Email is required")
    private String email;

    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @Size(max = 20, message = "Student ID must not exceed 20 characters")
    private String studentId;

    @Size(max = 100, message = "Full name must not exceed 100 characters")
    private String fullName;

    @Size(max = 20, message = "Role must not exceed 20 characters")
    private String role;

    @Size(max = 500, message = "Bio must not exceed 500 characters")
    private String bio;

    // Profile images
    @Property("avatarUrl")
    @Size(max = 500, message = "Avatar URL must not exceed 500 characters")
    private String avatarUrl;

    @Property("backgroundUrl")
    @Size(max = 500, message = "Background URL must not exceed 500 characters")
    private String backgroundUrl;

    @Builder.Default
    private Boolean isActive = true;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Relationships
    @Relationship(type = "ENROLLED_IN", direction = Relationship.Direction.OUTGOING)
    private MajorEntity major;

    @Relationship(type = "IN_BATCH", direction = Relationship.Direction.OUTGOING)
    private BatchEntity batch;

    @Relationship(type = "HAS_GENDER", direction = Relationship.Direction.OUTGOING)
    private GenderEntity gender;

    @Relationship(type = "IS_FRIENDS_WITH") // Removed direction = Relationship.Direction.OUTGOING
    @Builder.Default
    private Set<UserEntity> friends = new HashSet<>();

    @Relationship(type = "SENT_FRIEND_REQUEST_TO", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private Set<UserEntity> sentFriendRequests = new HashSet<>();

    @Relationship(type = "SENT_FRIEND_REQUEST_TO", direction = Relationship.Direction.INCOMING)
    @Builder.Default
    private Set<UserEntity> receivedFriendRequests = new HashSet<>();

    // Factory method for creating from auth service
    public static UserEntity fromAuthService(String authUserId, String email, String username, String role) {
        return UserEntity.builder()
                .id(authUserId)
                .email(email)
                .username(username != null ? username : "")
                .role(role)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // Business logic methods
    public void updateProfile(String fullName, String bio, String studentId) {
        this.fullName = fullName;
        this.bio = bio;
        this.studentId = studentId;
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.isActive = false;
        this.updatedAt = LocalDateTime.now();
    }

    public void activate() {
        this.isActive = true;
        this.updatedAt = LocalDateTime.now();
    }

    public boolean canSendFriendRequest(UserEntity target) {
        return !this.equals(target) &&
               !friends.contains(target) &&
               !sentFriendRequests.contains(target) &&
               !receivedFriendRequests.contains(target);
    }

    public void sendFriendRequest(UserEntity target) {
        if (canSendFriendRequest(target)) {
            sentFriendRequests.add(target);
            target.receivedFriendRequests.add(this);
        }
    }

    public void acceptFriendRequest(UserEntity requester) {
        if (receivedFriendRequests.contains(requester)) {
            receivedFriendRequests.remove(requester);
            requester.sentFriendRequests.remove(this);

            friends.add(requester);
            requester.friends.add(this);
        }
    }

    public void rejectFriendRequest(UserEntity requester) {
        if (receivedFriendRequests.contains(requester)) {
            receivedFriendRequests.remove(requester);
            requester.sentFriendRequests.remove(this);
        }
    }

    public void removeFriend(UserEntity friend) {
        if (friends.contains(friend)) {
            friends.remove(friend);
            friend.friends.remove(this);
        }
    }

    // Safe getter methods
    public Boolean getIsActive() {
        return isActive != null ? isActive : true;
    }

    public boolean isActive() {
        return getIsActive();
    }

    public String getUsername() {
        return username != null ? username : "";
    }

    public String getBio() {
        return bio != null ? bio : "";
    }

    public String getFullName() {
        return fullName != null ? fullName : "";
    }

    public String getStudentId() {
        return studentId != null ? studentId : "";
    }

    public String getRole() {
        return role != null ? role : "USER";
    }

    public Set<UserEntity> getFriends() {
        return friends != null ? friends : new HashSet<>();
    }

    public Set<UserEntity> getSentFriendRequests() {
        return sentFriendRequests != null ? sentFriendRequests : new HashSet<>();
    }

    public Set<UserEntity> getReceivedFriendRequests() {
        return receivedFriendRequests != null ? receivedFriendRequests : new HashSet<>();
    }

    // Utility methods
    public boolean hasSameMajor(UserEntity other) {
        return this.major != null && other.major != null &&
               this.major.getName().equals(other.major.getName());
    }

    public boolean hasSameFaculty(UserEntity other) {
        return this.major != null && other.major != null &&
               this.major.getFaculty() != null && other.major.getFaculty() != null &&
               this.major.getFaculty().getName().equals(other.major.getFaculty().getName());
    }

    public boolean hasSameCollege(UserEntity other) {
        return this.major != null && other.major != null &&
               this.major.getFaculty() != null && other.major.getFaculty() != null &&
               this.major.getFaculty().getCollege() != null && other.major.getFaculty().getCollege() != null &&
               this.major.getFaculty().getCollege().getName().equals(other.major.getFaculty().getCollege().getName());
    }

    public boolean hasSameBatch(UserEntity other) {
        return this.batch != null && other.batch != null &&
               this.batch.getYear().equals(other.batch.getYear());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserEntity that = (UserEntity) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    // Helper methods for accessing nested properties
    public String getAvatarUrl() {
        // Default avatar implementation - can be enhanced to store actual avatar URL
        return null;
    }

    public String getFacultyId() {
        return major != null && major.getFaculty() != null ? major.getFaculty().getCode() : null;
    }

    public String getFacultyName() {
        return major != null && major.getFaculty() != null ? major.getFaculty().getName() : null;
    }

    public String getMajorId() {
        return major != null ? major.getName() : null;
    }

    public String getMajorName() {
        return major != null ? major.getName() : null;
    }

    public String getBatchId() {
        return batch != null && batch.getYear() != null ? String.valueOf(batch.getYear()) : null;
    }

    public Integer getBatchYear() {
        return batch != null ? batch.getYear() : null;
    }
}