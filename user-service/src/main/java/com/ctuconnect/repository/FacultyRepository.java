package com.ctuconnect.repository;

import com.ctuconnect.entity.FacultyEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FacultyRepository extends Neo4jRepository<FacultyEntity, String> {

    Optional<FacultyEntity> findByName(String name);

    Optional<FacultyEntity> findByCode(String code);

    boolean existsByName(String name);

    boolean existsByCode(String code);

    @Query("""
        MATCH (f:Faculty)-[:HAS_FACULTY]-(c:College {name: $collegeName})
        RETURN f
        ORDER BY f.name ASC
        """)
    List<FacultyEntity> findByCollegeName(@Param("collegeName") String collegeName);

    @Query("""
        MATCH (f:Faculty)-[:HAS_FACULTY]-(c:College {name: $collegeName})
        RETURN f
        ORDER BY f.name ASC
        """)
    List<FacultyEntity> findFlatByCollegeName(@Param("collegeName") String collegeName);

    @Query("""
        MATCH (f:Faculty)
        OPTIONAL MATCH (f)-[:HAS_FACULTY]-(c:College)
        OPTIONAL MATCH (f)-[:HAS_MAJOR]->(m:Major)
        RETURN f, c, collect(m) as majors
        ORDER BY f.name ASC
        """)
    List<FacultyEntity> findAllWithRelations();

    // Alias for findAllWithRelations
    default List<FacultyEntity> findAllWithCollegeAndMajors() {
        return findAllWithRelations();
    }

    // Alias for findByCollegeName
    default List<FacultyEntity> findByCollege(String collegeName) {
        return findByCollegeName(collegeName);
    }

    @Query("""
        MATCH (f:Faculty {name: $name})
        OPTIONAL MATCH (f)-[:HAS_FACULTY]-(c:College)
        OPTIONAL MATCH (f)-[:HAS_MAJOR]->(m:Major)
        RETURN f, c, collect(m) as majors
        """)
    Optional<FacultyEntity> findByNameWithCollegeAndMajors(@Param("name") String name);
}
