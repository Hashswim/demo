package com.staffing.platform;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.Map;
import java.util.List;
@Entity @Table(name="analysis_jobs")
public class JobEntity {
    @Id public String id;
    @Column(name="request_id",nullable=false) public String requestId;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition="jsonb",nullable=false) public Map<String,Object> payload;
    @Column(nullable=false) public String status;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition="jsonb") public List<Map<String,Object>> result;
    public String error;
    public int attempts;
    @Column(name="lease_until") public Instant leaseUntil;
    @Column(name="created_at",nullable=false) public Instant createdAt;
    @Column(name="updated_at",nullable=false) public Instant updatedAt;
    protected JobEntity() {}
    public Models.JobView view() { return new Models.JobView(id,requestId,status,result,error); }
}
