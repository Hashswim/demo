package com.staffing.platform;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name="shortlists")
public class ShortlistEntity {
    @Id public String id;
    @Column(name="request_id",nullable=false) public String requestId;
    @Column(name="employee_id",nullable=false) public String employeeId;
    @Column(nullable=false) public String owner;
    @Column(nullable=false,length=2000) public String reason;
    @Column(name="created_at",nullable=false) public Instant createdAt;
    protected ShortlistEntity() {}
}
