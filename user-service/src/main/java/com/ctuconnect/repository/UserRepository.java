package com.ctuconnect.repository;

import com.ctuconnect.entity.UserEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends Neo4jRepository<UserEntity, String> {

    // Basic user queries
    Optional<UserEntity> findByEmail(String email);

    boolean existsByEmail(String email);
    
    // Fallback query to fetch avatarUrl separately
    @Query("""
        MATCH (u:User {id: $userId})
        RETURN u.avatarUrl as avatarUrl
        """)
    String findAvatarUrlById(@Param("userId") String userId);

    Optional<UserEntity> findByStudentId(String studentId);

    boolean existsByStudentId(String studentId);

    List<UserEntity> findByIsActiveTrue();

    // Simple user profile query - fetch user with all direct relationships
    // Explicitly return all properties to ensure avatarUrl is loaded
    @Query("""
        MATCH (u:User {id: $userId})
        OPTIONAL MATCH (u)-[:ENROLLED_IN]->(m:Major)
        OPTIONAL MATCH (u)-[:IN_BATCH]->(b:Batch)
        OPTIONAL MATCH (u)-[:HAS_GENDER]->(g:Gender)
        RETURN u, m, b, g
        """)
    Optional<UserEntity> findUserWithRelationships(@Param("userId") String userId);

    // Simple user search - returns UserEntity directly
    @Query("""
    MATCH (u:User)
    WHERE u.isActive = true
    AND (
        toLower(u.fullName) CONTAINS toLower($searchTerm) OR
        toLower(u.username) CONTAINS toLower($searchTerm) OR
        toLower(u.email) CONTAINS toLower($searchTerm) OR
        toLower(u.studentId) CONTAINS toLower($searchTerm)
    )
    AND ($currentUserId IS NULL OR u.id <> $currentUserId)
    RETURN u
    ORDER BY u.fullName ASC
    """)
    List<UserEntity> searchUsers(@Param("searchTerm") String searchTerm,
                                  @Param("currentUserId") String currentUserId);

    // Find friends of a user - returns UserEntity directly
    @Query("""
    MATCH (u:User {id: $userId})-[:IS_FRIENDS_WITH]->(friend:User)
    WHERE friend.isActive = true
    RETURN friend
    ORDER BY friend.fullName ASC
    """)
    List<UserEntity> findFriends(@Param("userId") String userId);

    // Find sent friend requests - returns UserEntity directly
    @Query("""
        MATCH (u:User {id: $userId})-[:SENT_FRIEND_REQUEST_TO]->(target:User)
        WHERE target.isActive = true
        RETURN target
        ORDER BY target.fullName ASC
        """)
    List<UserEntity> findSentFriendRequests(@Param("userId") String userId);

    // Find received friend requests - returns UserEntity directly
    @Query("""
        MATCH (requester:User)-[:SENT_FRIEND_REQUEST_TO]->(u:User {id: $userId})
        WHERE requester.isActive = true
        RETURN requester
        ORDER BY requester.fullName ASC
        """)
    List<UserEntity> findReceivedFriendRequests(@Param("userId") String userId);

    // Send friend request
    @Query("""
        MATCH (sender:User {id: $senderId}), (receiver:User {id: $receiverId})
        WHERE sender.isActive = true AND receiver.isActive = true
        AND NOT (sender)-[:IS_FRIENDS_WITH]-(receiver)
        AND NOT (sender)-[:SENT_FRIEND_REQUEST_TO]->(receiver)
        AND NOT (receiver)-[:SENT_FRIEND_REQUEST_TO]->(sender)
        CREATE (sender)-[:SENT_FRIEND_REQUEST_TO]->(receiver)
        RETURN count(*) > 0 as success
        """)
    boolean sendFriendRequest(@Param("senderId") String senderId, @Param("receiverId") String receiverId);

    // Accept friend request - Fix: Neo4j requires directed relationships in CREATE
    @Query("""
        MATCH (requester:User {id: $requesterId})-[r:SENT_FRIEND_REQUEST_TO]->(accepter:User {id: $accepterId})
        WHERE requester.isActive = true AND accepter.isActive = true
        DELETE r
        CREATE (requester)-[:IS_FRIENDS_WITH]->(accepter)
        CREATE (accepter)-[:IS_FRIENDS_WITH]->(requester)
        RETURN count(*) > 0 as success
        """)
    boolean acceptFriendRequest(@Param("requesterId") String requesterId, @Param("accepterId") String accepterId);

    // Reject/Cancel friend request - supports both directions
    // Can be used by receiver to reject OR sender to cancel
    @Query("""
        MATCH (user1:User {id: $userId1})
        MATCH (user2:User {id: $userId2})
        OPTIONAL MATCH (user1)-[r1:SENT_FRIEND_REQUEST_TO]->(user2)
        OPTIONAL MATCH (user2)-[r2:SENT_FRIEND_REQUEST_TO]->(user1)
        WITH r1, r2
        WHERE r1 IS NOT NULL OR r2 IS NOT NULL
        DELETE r1, r2
        RETURN count(r1) + count(r2) > 0 as success
        """)
    boolean rejectFriendRequest(@Param("userId1") String userId1, @Param("userId2") String userId2);

    // Remove friend
    @Query("""
        MATCH (user1:User {id: $userId1})-[r:IS_FRIENDS_WITH]-(user2:User {id: $userId2})
        DELETE r
        RETURN count(*) > 0 as success
        """)
    boolean removeFriend(@Param("userId1") String userId1, @Param("userId2") String userId2);

    // Find users by college - returns UserEntity directly
    @Query("""
    MATCH (u:User)-[:ENROLLED_IN]->(m:Major)-[:HAS_MAJOR]-(f:Faculty)-[:HAS_FACULTY]-(c:College {name: $collegeName})
    WHERE u.isActive = true AND ($currentUserId IS NULL OR u.id <> $currentUserId)
    RETURN u
    ORDER BY u.fullName ASC
    """)
    List<UserEntity> findUsersByCollege(@Param("collegeName") String collegeName,
                                        @Param("currentUserId") String currentUserId);

    // Find users by faculty - returns UserEntity directly
    @Query("""
    MATCH (u:User)-[:ENROLLED_IN]->(m:Major)-[:HAS_MAJOR]-(f:Faculty {name: $facultyName})
    WHERE u.isActive = true AND ($currentUserId IS NULL OR u.id <> $currentUserId)
    RETURN u
    ORDER BY u.fullName ASC
    """)
    List<UserEntity> findUsersByFaculty(@Param("facultyName") String facultyName,
                                        @Param("currentUserId") String currentUserId);

    // Simple query to get users by faculty (returns UserEntity directly)
    @Query("""
    MATCH (u:User)-[:ENROLLED_IN]->(m:Major)-[:HAS_MAJOR]-(f:Faculty {name: $facultyName})
    WHERE u.isActive = true AND u.id <> $currentUserId
    RETURN u
    ORDER BY u.fullName ASC
    """)
    List<UserEntity> findSimpleUsersByFaculty(@Param("facultyName") String facultyName,
                                              @Param("currentUserId") String currentUserId,
                                              Pageable pageable);

    // Find users by major - returns UserEntity directly
    @Query("""
    MATCH (u:User)-[:ENROLLED_IN]->(m:Major {name: $majorName})
    WHERE u.isActive = true AND ($currentUserId IS NULL OR u.id <> $currentUserId)
    RETURN u
    ORDER BY u.fullName ASC
    """)
    List<UserEntity> findUsersByMajor(@Param("majorName") String majorName,
                                      @Param("currentUserId") String currentUserId);

    // Find users by batch - returns UserEntity directly
    @Query("""
    MATCH (u:User)-[:IN_BATCH]->(b:Batch {year: $batchYear})
    WHERE u.isActive = true AND ($currentUserId IS NULL OR u.id <> $currentUserId)
    RETURN u
    ORDER BY u.fullName ASC
    """)
    List<UserEntity> findUsersByBatch(@Param("batchYear") String batchYear,
                                      @Param("currentUserId") String currentUserId);

    // Get random active users (for when no filters provided)
    @Query("""
        MATCH (currentUser:User {id: $currentUserId})
        MATCH (u:User)
        WHERE u.isActive = true 
        AND u.id <> currentUser.id
        AND NOT (currentUser)-[:IS_FRIENDS_WITH]-(u)
        AND NOT (currentUser)-[:SENT_FRIEND_REQUEST_TO]->(u)
        AND NOT (u)-[:SENT_FRIEND_REQUEST_TO]->(currentUser)
        RETURN u
        ORDER BY rand()
        LIMIT $limit
        """)
    List<UserEntity> findRandomUsers(@Param("currentUserId") String currentUserId, 
                                     @Param("limit") int limit);

    // Friend suggestion query based on mutual friends
    @Query("""
        MATCH (u:User {id: $userId})-[:IS_FRIENDS_WITH]-(friend:User)-[:IS_FRIENDS_WITH]-(suggestion:User)
        WHERE suggestion.isActive = true 
        AND suggestion.id <> $userId
        AND NOT (u)-[:IS_FRIENDS_WITH]-(suggestion)
        AND NOT (u)-[:SENT_FRIEND_REQUEST_TO]->(suggestion)
        AND NOT (suggestion)-[:SENT_FRIEND_REQUEST_TO]->(u)
        RETURN DISTINCT suggestion
        ORDER BY suggestion.fullName ASC
        LIMIT 50
        """)
    List<UserEntity> findFriendSuggestions(@Param("userId") String userId);

    // Check if two users are friends
    @Query("""
        MATCH (u1:User {id: $userId1})-[:IS_FRIENDS_WITH]-(u2:User {id: $userId2})
        RETURN count(*) > 0
        """)
    boolean areFriends(@Param("userId1") String userId1, @Param("userId2") String userId2);

    // Find mutual friends between two users
    @Query("""
        MATCH (u1:User {id: $userId1})-[:IS_FRIENDS_WITH]-(mutual:User)-[:IS_FRIENDS_WITH]-(u2:User {id: $userId2})
        WHERE mutual.isActive = true
        RETURN DISTINCT mutual
        ORDER BY mutual.fullName ASC
        """)
    List<UserEntity> findMutualFriends(@Param("userId1") String userId1, @Param("userId2") String userId2);

    // Check if there's a pending friend request between two users
    @Query("""
        MATCH (u1:User {id: $userId1})-[:SENT_FRIEND_REQUEST_TO]->(u2:User {id: $userId2})
        RETURN count(*) > 0
        """)
    boolean hasPendingFriendRequest(@Param("userId1") String userId1, @Param("userId2") String userId2);

    // Find users by faculty ID (name)
    @Query("""
        MATCH (u:User)-[:ENROLLED_IN]->(m:Major)-[:HAS_MAJOR]-(f:Faculty {name: $facultyId})
        WHERE u.isActive = true
        RETURN DISTINCT u
        ORDER BY u.fullName ASC
        """)
    List<UserEntity> findUsersByFacultyId(@Param("facultyId") String facultyId);

    // Find users by major ID (name)
    @Query("""
        MATCH (u:User)-[:ENROLLED_IN]->(m:Major {name: $majorId})
        WHERE u.isActive = true
        RETURN DISTINCT u
        ORDER BY u.fullName ASC
        """)
    List<UserEntity> findUsersByMajorId(@Param("majorId") String majorId);

    // Update relationship methods - properly handle Neo4j relationship updates
    
    @Query("""
        MATCH (u:User {id: $userId})
        OPTIONAL MATCH (u)-[r:ENROLLED_IN]->(:Major)
        DELETE r
        WITH u
        MATCH (m:Major {name: $majorName})
        MERGE (u)-[:ENROLLED_IN]->(m)
        """)
    void updateUserMajor(@Param("userId") String userId, @Param("majorName") String majorName);
    
    @Query("""
        MATCH (u:User {id: $userId})
        OPTIONAL MATCH (u)-[r:IN_BATCH]->(:Batch)
        DELETE r
        WITH u
        MATCH (b:Batch {year: $batchYear})
        MERGE (u)-[:IN_BATCH]->(b)
        """)
    void updateUserBatch(@Param("userId") String userId, @Param("batchYear") Integer batchYear);
    
    @Query("""
        MATCH (u:User {id: $userId})
        OPTIONAL MATCH (u)-[r:HAS_GENDER]->(:Gender)
        DELETE r
        WITH u
        MATCH (g:Gender {name: $genderName})
        MERGE (u)-[:HAS_GENDER]->(g)
        """)
    void updateUserGender(@Param("userId") String userId, @Param("genderName") String genderName);
}

