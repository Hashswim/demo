package com.staffing.platform;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.*;

class OwnershipTest {
    @Test void foreignRequestIsNotDisclosed() {
        var requests=mock(RequestRepository.class);var r=new RequestEntity();r.id="r1";r.owner="alice";
        when(requests.findById("r1")).thenReturn(Optional.of(r));
        var service=new StaffingService(mock(EmployeeRepository.class),requests,mock(JobRepository.class),mock(ShortlistRepository.class));
        var error=assertThrows(ResponseStatusException.class,() -> service.ownedRequest("r1","bob"));assertEquals(404,error.getStatusCode().value());
    }
    @Test void duplicateAnalyzeReusesActiveJob() {
        var requests=mock(RequestRepository.class);var jobs=mock(JobRepository.class);var r=new RequestEntity();r.owner="alice";r.id="r1";
        when(requests.lockById("r1")).thenReturn(Optional.of(r));
        var j=new JobEntity();j.id="j1";j.requestId="r1";j.status="RUNNING";
        when(jobs.findFirstByRequestIdAndStatusInOrderByCreatedAtDesc(eq("r1"),anyCollection())).thenReturn(Optional.of(j));
        var service=new StaffingService(mock(EmployeeRepository.class),requests,jobs,mock(ShortlistRepository.class));
        assertEquals("j1",service.analyze("r1","alice").id());verify(jobs,never()).save(any());
    }
    @Test void refreshesAvailabilityWithoutRewritingEvidence() {
        var requests=mock(RequestRepository.class);var jobs=mock(JobRepository.class);var employees=mock(EmployeeRepository.class);
        var r=new RequestEntity();r.id="r1";r.owner="alice";when(requests.findById("r1")).thenReturn(Optional.of(r));
        var j=new JobEntity();j.id="j1";j.requestId="r1";j.status="SUCCEEDED";
        j.result=List.of(Map.of("employee",Map.of("id","e1","availability","AVAILABLE"),"evidence",List.of(Map.of("quote","saved source"))));
        when(jobs.findById("j1")).thenReturn(Optional.of(j));
        var current=new Models.Employee("e1","가상인력","개발자","제조",List.of("Java"),Models.Availability.UNKNOWN,null,0,List.of(),null);
        when(employees.findById("e1")).thenReturn(Optional.of(new EmployeeEntity(current)));
        var service=new StaffingService(employees,requests,jobs,mock(ShortlistRepository.class));
        var first=(Map<?,?>)((List<?>)service.job("j1","alice").result()).get(0);
        assertEquals(Models.Availability.UNKNOWN,((Models.Employee)first.get("employee")).availability());
        assertEquals(List.of(Map.of("quote","saved source")),first.get("evidence"));
    }
}
