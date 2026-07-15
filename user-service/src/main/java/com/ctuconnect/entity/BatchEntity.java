package com.ctuconnect.entity;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;


import jakarta.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;

@Node("Batch")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchEntity {
    @Id
    @NotNull(message = "Batch is required")
    private Integer year;

    private String description;



    // Safe getter methods
    public Integer getYear() {
        return year;
    }

    public String getDescription() {
        return description != null ? description : "";
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BatchEntity that = (BatchEntity) o;
        return year != null && year.equals(that.year);
    }

    @Override
    public int hashCode() {
        return year != null ? year.hashCode() : 0;
    }
}
