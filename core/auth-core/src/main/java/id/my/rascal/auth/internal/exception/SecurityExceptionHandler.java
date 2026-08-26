package id.my.rascal.auth.internal.exception;

import java.io.IOException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import id.my.rascal.auth.internal.service.RefreshTokenCookieFactory;
import id.my.rascal.common.ApiResponse;
import id.my.rascal.common.exception.UnauthorizedException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityExceptionHandler implements 
    AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final RefreshTokenCookieFactory refreshTokenCookieFactory;

    public SecurityExceptionHandler(
        ObjectMapper objectMapper,
        RefreshTokenCookieFactory refreshTokenCookieFactory
    ) {
        this.objectMapper = objectMapper;
        this.refreshTokenCookieFactory = refreshTokenCookieFactory;
    }

    @Override
    public void commence(
        HttpServletRequest request, 
        HttpServletResponse response,
        AuthenticationException ex
    ) throws IOException, ServletException {
        writeError(
            response, request, 
            401, HttpStatus.UNAUTHORIZED,
            "UNAUTHORIZED", ex.getMessage()    
        );
    }

    @Override
    public void handle(
        HttpServletRequest request, 
        HttpServletResponse response,
        AccessDeniedException ex
    ) throws IOException, ServletException {
        writeError(
            response, request, 
            403, HttpStatus.FORBIDDEN,
            "FORBIDDEN", ex.getMessage()    
        );
       
    }

    private void writeError(
        HttpServletResponse response, 
        HttpServletRequest request,
        int status, HttpStatus httpStatus,
        String errorType, String message
    ) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(
            ApiResponse.errorBody(status, errorType, message)
        ));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<?> handleAuth(AuthenticationException ex) {
        return ApiResponse.error(HttpStatus.UNAUTHORIZED, 401, "UNAUTHORIZED", ex.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<?> handleAccessDenied(AccessDeniedException ex) {
        return ApiResponse.error(HttpStatus.FORBIDDEN, 403, "FORBIDDEN", ex.getMessage());
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<?> handleAuthorizationDenied(AuthorizationDeniedException ex) {
        return ApiResponse.error(HttpStatus.FORBIDDEN, 403, "FORBIDDEN", ex.getMessage());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<?> handleUnauthorized(UnauthorizedException ex) {
        ResponseCookie clearCookie = refreshTokenCookieFactory.clear();

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .header(
                HttpHeaders.SET_COOKIE,
                clearCookie.toString()
            ).body(
                ApiResponse.errorBody(
                    401, "INVALID_REFRESH_TOKEN", ex.getMessage()
                )
            );
    }

}
