package com.ctuconnect.repository;

import com.ctuconnect.entity.BatchEntity;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BatchRepository extends Neo4jRepository<BatchEntity, Integer> {

    Optional<BatchEntity> findByYear(Integer year);

    boolean existsByYear(Integer year);

    @Query("""
        MATCH (b:Batch)
        RETURN b
        ORDER BY b.year DESC
        """)
    List<BatchEntity> findAllFlat();
    @Query("""
        MATCH (b:Batch)
        WHERE b.year >= $startYear AND b.year <= $endYear
        RETURN b
        ORDER BY b.year DESC
        """)
    List<BatchEntity> findByYearRange(@Param("startYear") Integer startYear,
                                     @Param("endYear") Integer endYear);

    @Query("""
        MATCH (b:Batch)
        OPTIONAL MATCH (b)<-[:IN_BATCH]-(u:User)
        RETURN b, count(u) as studentCount
        ORDER BY b.year DESC
        """)
    List<BatchWithStudentCount> findAllWithStudentCounts();

    interface BatchWithStudentCount {
        BatchEntity getBatch();
        Long getStudentCount();
    }
}
