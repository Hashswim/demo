package com.staffing.platform;
import jakarta.persistence.*;
@Entity @Table(name="app_users")
public class AppUser {
    @Id public String username;
    @Column(name="password_hash",nullable=false) public String passwordHash;
    @Column(nullable=false) public String role;
    public boolean enabled=true;
    protected AppUser() {}
    public AppUser(String username,String passwordHash,String role) {this.username=username;this.passwordHash=passwordHash;this.role=role;}
}
