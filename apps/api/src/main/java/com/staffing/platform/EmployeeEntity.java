package com.staffing.platform;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Entity @Table(name="employees")
public class EmployeeEntity {
    @Id public String id;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition="jsonb", nullable=false) public Models.Employee data;
    @Version public long version;
    protected EmployeeEntity() {}
    public EmployeeEntity(Models.Employee data) { this.id=data.id(); this.data=data; }
}
