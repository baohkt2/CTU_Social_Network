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

@Node("Gender")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenderEntity {

    @NotBlank(message = "Gender name is required")
    @Size(max = 20, message = "Gender name must not exceed 20 characters")
    private String name;    

    @Id
    @NotBlank(message = "Gender code is required")
    @Size(max = 20, message = "Gender code must not exceed 20 characters")
    private String code;

    @Size(max = 100, message = "Description must not exceed 100 characters")
    private String description;



    // Safe getter methods
    public String getName() {
        return name != null ? name : "";
    }

    public String getDescription() {
        return description != null ? description : "";
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GenderEntity that = (GenderEntity) o;
        return name != null && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}
