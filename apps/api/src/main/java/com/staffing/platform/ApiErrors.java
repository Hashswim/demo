package com.staffing.platform;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import java.util.Map;

@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler(MethodArgumentNotValidException.class) ResponseEntity<?> invalid(MethodArgumentNotValidException ex) {return ResponseEntity.badRequest().body(Map.of("message","입력 항목을 확인해 주세요.","fields",ex.getBindingResult().getFieldErrors().stream().map(e -> e.getField()).distinct().toList()));}
    @ExceptionHandler(AuthenticationException.class) ResponseEntity<?> auth(AuthenticationException ex) {return ResponseEntity.status(401).body(Map.of("message","아이디와 비밀번호를 확인해 주세요."));}
    @ExceptionHandler(ResponseStatusException.class) ResponseEntity<?> status(ResponseStatusException ex) {return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message",ex.getReason()==null?"요청을 처리할 수 없습니다.":ex.getReason()));}
    @ExceptionHandler(RestClientException.class) ResponseEntity<?> upstream(RestClientException ex) {return ResponseEntity.status(503).body(Map.of("message","AI 서비스를 사용할 수 없습니다. 상태를 확인한 뒤 다시 시도해 주세요."));}
}
