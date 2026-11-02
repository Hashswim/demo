package com.staffing.platform;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
@Entity @Table(name="staffing_requests")
public class RequestEntity {
    @Id public String id;
    @Column(nullable=false) public String owner;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition="jsonb",nullable=false) public Models.StaffingRequest data;
    @Column(name="created_at",nullable=false) public Instant createdAt;
    protected RequestEntity() {}
}
