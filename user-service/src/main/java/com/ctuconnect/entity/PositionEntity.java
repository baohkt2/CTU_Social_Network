package com.ctuconnect.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node("Position")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PositionEntity {
    private String name; // Tên vị trí, ví dụ: "Giảng viên", "Trợ giảng", "Nhân viên hành chính", v.v.
    @Id
    private String code; // Mã định danh cho vị trí, ví dụ: "LECTURER", "TEACHING_ASSISTANT", "ADMIN_STAFF"

    private String description; // Mô tả chi tiết về vị trí

    public String getId() {
        return code;
    }
}
