package com.staffing.platform;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class Models {
    private Models() {}
    public enum Availability { AVAILABLE, ASSIGNED, UNKNOWN }
    public enum Kind { MUST, PREFERRED }
    public record Experience(String id, @NotBlank @Size(max=200) String title,
            @NotBlank @Size(max=100) String domain, @NotBlank @Size(max=100) String role,
            @NotNull LocalDate startDate, LocalDate endDate,
            @NotNull @Size(max=50) List<@NotBlank @Size(max=100) String> technologies,
            @NotBlank @Size(max=5000) String description) {}
    public record Assignment(@NotBlank String projectTitle, @NotNull LocalDate startDate, @NotNull LocalDate endDate) {}
    public record Employee(String id, @NotBlank @Size(max=100) String name,
            @NotBlank @Size(max=100) String title, @NotBlank @Size(max=100) String department,
            @NotNull @Size(max=50) List<@NotBlank @Size(max=100) String> skills,
            @NotNull Availability availability, LocalDate availableFrom,
            @Min(0) @Max(100) int allocation, @NotNull @Size(max=100) List<@Valid Experience> experiences, @Valid Assignment assignment) {}
    public record Requirement(String id, @NotBlank @Size(max=500) String text, @NotNull Kind kind) {}
    public record RequestInput(@NotBlank @Size(max=200) String title, @NotBlank @Size(max=100) String domain,
            @NotBlank @Size(max=10000) String description,
            @NotNull @Size(max=50) List<@NotBlank @Size(max=100) String> skills,
            @NotNull @Size(min=1,max=50) List<@Valid Requirement> requirements,
            LocalDate startDate, LocalDate endDate) {}
    public record StaffingRequest(String id, String title, String domain, String description,
            List<String> skills, List<Requirement> requirements, LocalDate startDate, LocalDate endDate, Instant createdAt) {}
    public record ParseInput(@NotBlank @Size(max=100) String domain, @NotBlank @Size(max=10000) String description,
            @NotNull @Size(max=50) List<@NotBlank @Size(max=100) String> skills) {}
    public record ShortlistInput(@NotBlank String requestId, @NotBlank String employeeId,
            @NotBlank @Size(max=2000) String reason) {}
    public record LoginInput(@NotBlank @Size(max=100) String username, @NotBlank @Size(max=200) String password) {}
    public record PrincipalView(String username, String role) {}
    public record JobView(String id, String requestId, String status, Object result, String error) {}
    public record ShortlistView(String id, String requestId, String employeeId, String reason, Instant createdAt, Employee employee) {}
}
