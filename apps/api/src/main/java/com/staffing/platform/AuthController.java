package com.staffing.platform;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.security.authentication.*;
import org.springframework.security.core.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.*;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager manager;
    private final HttpSessionSecurityContextRepository contexts;
    private final CsrfTokenRepository csrf;
    public AuthController(AuthenticationManager manager,HttpSessionSecurityContextRepository contexts,CsrfTokenRepository csrf) {this.manager=manager;this.contexts=contexts;this.csrf=csrf;}
    @GetMapping("/csrf") public Map<String,String> csrf(CsrfToken token) { return Map.of("token",token.getToken(),"headerName",token.getHeaderName()); }
    @PostMapping("/login") public Models.PrincipalView login(@Valid @RequestBody Models.LoginInput input,HttpServletRequest req,HttpServletResponse res) {
        var auth=manager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(input.username(),input.password()));
        if(req.getSession(false)!=null) req.changeSessionId();
        var context=SecurityContextHolder.createEmptyContext();context.setAuthentication(auth);SecurityContextHolder.setContext(context);contexts.saveContext(context,req,res);
        csrf.saveToken(null,req,res);
        return principal(auth);
    }
    @GetMapping("/me") public Models.PrincipalView me(Authentication auth) { return principal(auth); }
    static Models.PrincipalView principal(Authentication auth) { return new Models.PrincipalView(auth.getName(),auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).filter(a -> a.startsWith("ROLE_")).findFirst().orElse("ROLE_ACCOUNT").substring(5)); }
}
