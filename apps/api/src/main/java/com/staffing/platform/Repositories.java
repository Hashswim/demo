package com.staffing.platform;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
import java.util.*;
interface EmployeeRepository extends JpaRepository<EmployeeEntity,String> {}
interface UserRepository extends JpaRepository<AppUser,String> {}
interface RequestRepository extends JpaRepository<RequestEntity,String> {
    List<RequestEntity> findByOwnerOrderByCreatedAtDesc(String owner);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from RequestEntity r where r.id = :id") Optional<RequestEntity> lockById(String id);
}
interface JobRepository extends JpaRepository<JobEntity,String> {
    Optional<JobEntity> findFirstByRequestIdOrderByCreatedAtDesc(String requestId);
    Optional<JobEntity> findFirstByRequestIdAndStatusInOrderByCreatedAtDesc(String requestId,Collection<String> statuses);
}
interface ShortlistRepository extends JpaRepository<ShortlistEntity,String> {
    List<ShortlistEntity> findByOwnerOrderByCreatedAtDesc(String owner);
    List<ShortlistEntity> findByOwnerAndRequestIdOrderByCreatedAtDesc(String owner,String requestId);
    Optional<ShortlistEntity> findByOwnerAndRequestIdAndEmployeeId(String owner,String requestId,String employeeId);
}
