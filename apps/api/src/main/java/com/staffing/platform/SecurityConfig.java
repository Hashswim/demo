package com.staffing.platform;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.*;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean UserDetailsService users(UserRepository repository) {
        return name -> repository.findById(name).filter(user -> user.enabled)
                .map(user -> User.withUsername(user.username).password(user.passwordHash).roles(user.role).build())
                .orElseThrow(() -> new UsernameNotFoundException("계정을 확인해 주세요."));
    }
    @Bean AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception { return config.getAuthenticationManager(); }
    @Bean HttpSessionSecurityContextRepository securityContextRepository() { return new HttpSessionSecurityContextRepository(); }
    @Bean CsrfTokenRepository csrfTokenRepository() { return new HttpSessionCsrfTokenRepository(); }
    @Bean SecurityFilterChain security(HttpSecurity http, CsrfTokenRepository csrf, HttpSessionSecurityContextRepository contexts) throws Exception {
        return http.csrf(c -> c.csrfTokenRepository(csrf))
                .securityContext(c -> c.securityContextRepository(contexts))
                .authorizeHttpRequests(a -> a
                    .requestMatchers("/api/auth/csrf", "/api/auth/login", "/actuator/health", "/actuator/health/**").permitAll()
                    .requestMatchers(HttpMethod.POST,"/api/employees").hasRole("MANAGER")
                    .requestMatchers(HttpMethod.PUT,"/api/employees/**").hasRole("MANAGER")
                    .requestMatchers(HttpMethod.POST,"/api/requests", "/api/requests/*/analyze", "/api/shortlists").hasRole("ACCOUNT")
                    .requestMatchers(HttpMethod.DELETE,"/api/shortlists/**").hasRole("ACCOUNT")
                    .anyRequest().authenticated())
                .exceptionHandling(e -> e.authenticationEntryPoint((req,res,ex) -> {
                    res.setStatus(401);res.setContentType("application/json;charset=UTF-8");res.getWriter().write("{\"message\":\"로그인이 필요합니다.\"}");
                }).accessDeniedHandler((req,res,ex) -> {
                    res.setStatus(403);res.setContentType("application/json;charset=UTF-8");res.getWriter().write("{\"message\":\"권한 또는 CSRF 토큰을 확인해 주세요.\"}");
                }))
                .logout(l -> l.logoutUrl("/api/auth/logout").logoutSuccessHandler((req,res,auth) -> res.setStatus(HttpServletResponse.SC_NO_CONTENT)))
                .build();
    }
}
