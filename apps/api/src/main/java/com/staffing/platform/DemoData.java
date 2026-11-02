package com.staffing.platform;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.nio.file.*;
import java.util.List;

@Component @ConditionalOnProperty(name="app.demo.enabled",havingValue="true")
public class DemoData implements ApplicationRunner {
    private final UserRepository users;private final EmployeeRepository employees;private final ObjectMapper mapper;private final PasswordEncoder encoder;
    @Value("${app.demo.data-path}") String path;
    @Value("${app.demo.account-password}") String accountPassword;
    @Value("${app.demo.manager-password}") String managerPassword;
    public DemoData(UserRepository users,EmployeeRepository employees,ObjectMapper mapper,PasswordEncoder encoder) {this.users=users;this.employees=employees;this.mapper=mapper;this.encoder=encoder;}
    @Override @Transactional public void run(ApplicationArguments args) throws Exception {
        if(accountPassword.length()<12 || managerPassword.length()<12) throw new IllegalStateException("Demo seed requires APP_DEMO_ACCOUNT_PASSWORD and APP_DEMO_MANAGER_PASSWORD of 12+ characters.");
        if(!users.existsById("account")) users.save(new AppUser("account",encoder.encode(accountPassword),"ACCOUNT"));
        if(!users.existsById("manager")) users.save(new AppUser("manager",encoder.encode(managerPassword),"MANAGER"));
        if(employees.count()==0) {
            List<Models.Employee> data=mapper.readValue(Files.readString(Path.of(path)),new TypeReference<>() {});
            for(var employee:data) {StaffingService.validatePeriods(employee);employees.save(new EmployeeEntity(employee));}
        }
    }
}
