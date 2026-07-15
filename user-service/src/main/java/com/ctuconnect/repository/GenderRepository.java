package com.ctuconnect.repository;

import com.ctuconnect.entity.GenderEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.List;
import java.util.Optional;

public interface GenderRepository extends Neo4jRepository<GenderEntity, String> {

    Optional<GenderEntity> findByName(String name);
    
    Optional<GenderEntity> findByCode(String code);
    boolean existsByName(String name);

    @Query("""
        MATCH (g:Gender)
        RETURN g
        ORDER BY g.name ASC
        """)
    List<GenderEntity> findAllFlat();

    @Query("""
        MATCH (g:Gender)
        OPTIONAL MATCH (g)<-[:HAS_GENDER]-(u:User)
        RETURN g, count(u) as userCount
        ORDER BY g.name ASC
        """)
    List<GenderWithUserCount> findAllWithUserCounts();

    interface GenderWithUserCount {
        GenderEntity getGender();
        Long getUserCount();
    }
}
