package com.ctuconnect.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.List;

@Node("Degree")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DegreeEntity {
    private String name; // Tên bằng cấp, ví dụ: "Cử nhân", "Thạc sĩ", "Tiến sĩ"
    @Id
    private String code; // Mã định danh cho bằng cấp, ví dụ: "BACHELOR", "MASTER", "PHD"

    private String description; // Mô tả chi tiết về bằng cấp

    public String getId() {
        return code; // Sử dụng mã làm ID
    }
}
