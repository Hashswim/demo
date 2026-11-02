package com.staffing.platform;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api")
public class StaffingController {
    private final StaffingService service;
    private final AiClient ai;
    public StaffingController(StaffingService service,AiClient ai) {this.service=service;this.ai=ai;}
    @GetMapping("/employees") public List<Models.Employee> employees() {return service.employees();}
    @PostMapping("/employees") @ResponseStatus(HttpStatus.CREATED) public Models.Employee createEmployee(@Valid @RequestBody Models.Employee input) {return service.saveEmployee(null,input,true);}
    @PutMapping("/employees/{id}") public Models.Employee updateEmployee(@PathVariable String id,@Valid @RequestBody Models.Employee input) {return service.saveEmployee(id,input,false);}
    @GetMapping("/requests") public List<Models.StaffingRequest> requests(Authentication auth) {return service.requests(auth.getName());}
    @PostMapping("/requests") @ResponseStatus(HttpStatus.CREATED) public Models.StaffingRequest request(@Valid @RequestBody Models.RequestInput input,Authentication auth) {return service.createRequest(auth.getName(),input);}
    @PostMapping("/requirements/parse") public Object parse(@Valid @RequestBody Models.ParseInput input) {return ai.parse(input);}
    @PostMapping("/requests/{id}/analyze") @ResponseStatus(HttpStatus.ACCEPTED) public Models.JobView analyze(@PathVariable String id,Authentication auth) {return service.analyze(id,auth.getName());}
    @GetMapping("/requests/{id}/latest-analysis") public Models.JobView latest(@PathVariable String id,Authentication auth) {return service.latestAnalysis(id,auth.getName());}
    @GetMapping("/jobs/{id}") public Models.JobView job(@PathVariable String id,Authentication auth) {return service.job(id,auth.getName());}
    @GetMapping("/shortlists") public List<Models.ShortlistView> shortlists(@RequestParam(required=false) String requestId,Authentication auth) {return service.shortlists(requestId,auth.getName());}
    @PostMapping("/shortlists") @ResponseStatus(HttpStatus.CREATED) public Models.ShortlistView shortlist(@Valid @RequestBody Models.ShortlistInput input,Authentication auth) {return service.addShortlist(input,auth.getName());}
    @DeleteMapping("/shortlists/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable String id,Authentication auth) {service.deleteShortlist(id,auth.getName());}
    @GetMapping("/system/status") public Object status() {return ai.health();}
    @GetMapping("/evaluations") public Object evaluations() {return ai.evaluations();}
}
