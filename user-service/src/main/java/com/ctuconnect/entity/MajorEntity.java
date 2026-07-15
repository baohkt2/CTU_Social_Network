package com.ctuconnect.entity;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.HashSet;
import java.util.Set;

@Node("Major")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MajorEntity {
   
    @NotBlank(message = "Major name is required")
    @Size(max = 100, message = "Major name must not exceed 100 characters")
    private String name;
    
    @Id
    @Size(max = 10, message = "Major code must not exceed 10 characters")
    private String code;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Relationship(type = "HAS_MAJOR", direction = Relationship.Direction.INCOMING)
    private FacultyEntity faculty;



    // Safe getter methods
    public String getName() {
        return name != null ? name : "";
    }

    public String getCode() {
        return code != null ? code : "";
    }

    public String getDescription() {
        return description != null ? description : "";
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MajorEntity that = (MajorEntity) o;
        return name != null && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}
