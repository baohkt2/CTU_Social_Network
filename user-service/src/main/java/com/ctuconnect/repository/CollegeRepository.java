package com.ctuconnect.repository;

import com.ctuconnect.entity.CollegeEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CollegeRepository extends Neo4jRepository<CollegeEntity, String> {

    Optional<CollegeEntity> findByName(String name);

    Optional<CollegeEntity> findByCode(String code);

    boolean existsByName(String name);

    boolean existsByCode(String code);

    @Query("""
        MATCH (c:College)-[:HAS_COLLEGE]-(u:University {name: $universityName})
        RETURN c
        ORDER BY c.name ASC
        """)
    List<CollegeEntity> findByUniversityName(@Param("universityName") String universityName);

    @Query("""
        MATCH (c:College)
        RETURN c
        ORDER BY c.name ASC
        """)
    List<CollegeEntity> findAllFlat();

    @Query("""
        MATCH (c:College)
        OPTIONAL MATCH (c)-[:HAS_COLLEGE]-(u:University)
        OPTIONAL MATCH (c)-[:HAS_FACULTY]->(f:Faculty)
        RETURN c, u, collect(f) as faculties
        ORDER BY c.name ASC
        """)
    List<CollegeEntity> findAllWithRelations();
}
