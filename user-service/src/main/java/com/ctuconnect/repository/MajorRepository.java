package com.ctuconnect.repository;

import com.ctuconnect.entity.MajorEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MajorRepository extends Neo4jRepository<MajorEntity, String> {

    Optional<MajorEntity> findByName(String name);

    Optional<MajorEntity> findByCode(String code);

    boolean existsByName(String name);

    boolean existsByCode(String code);

    @Query("""
        MATCH (m:Major)-[:HAS_MAJOR]-(f:Faculty {name: $facultyName})
        RETURN m
        ORDER BY m.name ASC
        """)
    List<MajorEntity> findByFacultyName(@Param("facultyName") String facultyName);

    @Query("""
        MATCH (m:Major)-[:HAS_MAJOR]-(f:Faculty {name: $facultyName})
        RETURN m
        ORDER BY m.name ASC
        """)
    List<MajorEntity> findFlatByFacultyName(@Param("facultyName") String facultyName);

    @Query("""
        MATCH (m:Major)-[:HAS_MAJOR]-(f:Faculty)-[:HAS_FACULTY]-(c:College {name: $collegeName})
        RETURN m
        ORDER BY m.name ASC
        """)
    List<MajorEntity> findByCollegeName(@Param("collegeName") String collegeName);

    @Query("""
        MATCH (m:Major)
        OPTIONAL MATCH (m)-[:HAS_MAJOR]-(f:Faculty)
        OPTIONAL MATCH (f)-[:HAS_FACULTY]-(c:College)
        RETURN m, f, c
        ORDER BY m.name ASC
        """)
    List<MajorEntity> findAllWithRelations();
}
