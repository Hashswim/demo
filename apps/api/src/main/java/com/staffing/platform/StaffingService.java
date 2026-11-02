package com.staffing.platform;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.time.Instant;
import java.util.*;

@Service
public class StaffingService {
    private final EmployeeRepository employees;
    private final RequestRepository requests;
    private final JobRepository jobs;
    private final ShortlistRepository shortlists;
    public StaffingService(EmployeeRepository employees,RequestRepository requests,JobRepository jobs,ShortlistRepository shortlists) {this.employees=employees;this.requests=requests;this.jobs=jobs;this.shortlists=shortlists;}
    public List<Models.Employee> employees() { return employees.findAll().stream().map(e -> e.data).sorted(Comparator.comparing(Models.Employee::name)).toList(); }
    @Transactional public Models.Employee saveEmployee(String id,Models.Employee input,boolean create) {
        validatePeriods(input);
        if(!create && !employees.existsById(id)) throw notFound();
        if(create) id=UUID.randomUUID().toString();
        var data=new Models.Employee(id,input.name(),input.title(),input.department(),input.skills(),input.availability(),input.availableFrom(),input.allocation(),input.experiences(),input.assignment());
        var entity=create?new EmployeeEntity(data):employees.findById(id).orElseThrow(StaffingService::notFound);entity.data=data;employees.save(entity);return data;
    }
    static void validatePeriods(Models.Employee employee) {
        for(var p:employee.experiences()) {
            if(p.id()==null || p.id().isBlank()) throw badRequest("프로젝트 이력 ID가 필요합니다.");
            if(p.startDate()!=null && p.endDate()!=null && p.endDate().isBefore(p.startDate())) throw badRequest("이력 종료일은 시작일 이후여야 합니다.");
        }
        if(employee.assignment()!=null && employee.assignment().endDate().isBefore(employee.assignment().startDate())) throw badRequest("투입 종료일은 시작일 이후여야 합니다.");
        if(employee.availability()==Models.Availability.AVAILABLE && employee.allocation()>0) throw badRequest("즉시 가능 상태와 투입률이 일치하지 않습니다.");
    }
    public List<Models.StaffingRequest> requests(String owner) { return requests.findByOwnerOrderByCreatedAtDesc(owner).stream().map(r -> r.data).toList(); }
    @Transactional public Models.StaffingRequest createRequest(String owner,Models.RequestInput input) {
        if(input.startDate()!=null && input.endDate()!=null && input.endDate().isBefore(input.startDate())) throw badRequest("요청 종료일은 시작일 이후여야 합니다.");
        if(input.requirements().stream().anyMatch(r -> r.id()==null || r.id().isBlank()) || input.requirements().stream().map(Models.Requirement::id).distinct().count()!=input.requirements().size()) throw badRequest("요건 ID는 고유해야 합니다.");
        var r=new RequestEntity();r.id=UUID.randomUUID().toString();r.owner=owner;r.createdAt=Instant.now();r.data=new Models.StaffingRequest(r.id,input.title(),input.domain(),input.description(),input.skills(),input.requirements(),input.startDate(),input.endDate(),r.createdAt);requests.save(r);return r.data;
    }
    RequestEntity ownedRequest(String id,String owner) {var r=requests.findById(id).orElseThrow(StaffingService::notFound);requireOwner(r.owner,owner);return r;}
    @Transactional public Models.JobView analyze(String id,String owner) {
        var r=requests.lockById(id).orElseThrow(StaffingService::notFound);requireOwner(r.owner,owner);
        var existing=jobs.findFirstByRequestIdAndStatusInOrderByCreatedAtDesc(id,List.of("QUEUED","RUNNING"));
        if(existing.isPresent()) return existing.get().view();
        var j=new JobEntity();j.id=UUID.randomUUID().toString();j.requestId=id;j.payload=Map.of("request",r.data,"employees",employees());j.status="QUEUED";j.attempts=0;j.createdAt=Instant.now();j.updatedAt=j.createdAt;jobs.save(j);return j.view();
    }
    public Models.JobView latestAnalysis(String id,String owner) {ownedRequest(id,owner);return jobs.findFirstByRequestIdOrderByCreatedAtDesc(id).map(j->job(j.id,owner)).orElse(null);}
    public Models.JobView job(String id,String owner) {var j=jobs.findById(id).orElseThrow(StaffingService::notFound);ownedRequest(j.requestId,owner);Object current=j.result;
        if(j.result!=null) {
            var rows=j.result;
            var refreshed=new ArrayList<Map<String,Object>>();
            for(Map<String,Object> map:rows) {
                var copy=new LinkedHashMap<String,Object>();map.forEach((k,v)->copy.put(String.valueOf(k),v));
                if(map.get("employee") instanceof Map<?,?> profile) {
                    employees.findById(String.valueOf(profile.get("id"))).ifPresent(e->copy.put("employee",e.data));
                }
                refreshed.add(copy);
            }
            current=refreshed;
        }
        return new Models.JobView(j.id,j.requestId,j.status,current,j.error);}
    public List<Models.ShortlistView> shortlists(String requestId,String owner) {
        if(requestId!=null) ownedRequest(requestId,owner);
        return (requestId==null?shortlists.findByOwnerOrderByCreatedAtDesc(owner):shortlists.findByOwnerAndRequestIdOrderByCreatedAtDesc(owner,requestId)).stream().map(this::shortlistView).toList();
    }
    @Transactional public Models.ShortlistView addShortlist(Models.ShortlistInput input,String owner) {
        ownedRequest(input.requestId(),owner);employees.findById(input.employeeId()).orElseThrow(StaffingService::notFound);
        var s=shortlists.findByOwnerAndRequestIdAndEmployeeId(owner,input.requestId(),input.employeeId()).orElseGet(ShortlistEntity::new);
        if(s.id==null) {s.id=UUID.randomUUID().toString();s.createdAt=Instant.now();}
        s.owner=owner;s.requestId=input.requestId();s.employeeId=input.employeeId();s.reason=input.reason().strip();
        return shortlistView(shortlists.save(s));
    }
    @Transactional public void deleteShortlist(String id,String owner) {var s=shortlists.findById(id).orElseThrow(StaffingService::notFound);requireOwner(s.owner,owner);shortlists.delete(s);}
    Models.ShortlistView shortlistView(ShortlistEntity s) {return new Models.ShortlistView(s.id,s.requestId,s.employeeId,s.reason,s.createdAt,employees.findById(s.employeeId).orElseThrow(StaffingService::notFound).data);}
    static void requireOwner(String actual,String supplied) {if(!Objects.equals(actual,supplied)) throw notFound();}
    static ResponseStatusException notFound() {return new ResponseStatusException(HttpStatus.NOT_FOUND,"항목을 찾을 수 없습니다.");}
    static ResponseStatusException badRequest(String message) {return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
}
