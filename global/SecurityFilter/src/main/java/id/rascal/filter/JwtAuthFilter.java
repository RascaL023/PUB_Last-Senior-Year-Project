package id.rascal.filter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.web.filter.OncePerRequestFilter;

import id.rascal.filter.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.ObjectMapper;

public class JwtAuthFilter extends OncePerRequestFilter implements
    AuthenticationEntryPoint, AccessDeniedHandler{

    private final ObjectMapper objectMapper;
    private JwtService jwtService;

    public JwtAuthFilter(
        ObjectMapper objectMapper,
        JwtService jwtService
    ) {
        this.objectMapper = objectMapper;
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request, 
        HttpServletResponse response, 
        FilterChain filterChain
    ) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7).trim();
            System.out.println("[FILTER]: Token gained => " + token);

            try {
                UsernamePasswordAuthenticationToken auth = proceedToken(token);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (ExpiredJwtException e) {
                System.out.println(e.getMessage());
                commence(
                    request, response, 
                    new BadCredentialsException("Expired token")
                ); return;
            } catch (JwtException e) {
                System.out.println(e.getMessage());
                commence(
                    request, response, 
                    new BadCredentialsException("Invalid token")
                ); return;
            }
        }

        filterChain.doFilter(request, response);
        
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
            "Unauthorized", 
            ex.getMessage()
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
            "Forbidden", 
            ex.getMessage()
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

        LinkedHashMap<String, Object> body = new LinkedHashMap<>();
        body.put("httpStatus", httpStatus);
        body.put("status", status);
        body.put("errorType", errorType);
        body.put("message", message);
        body.put("timestamp", LocalDateTime.now());

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }

    private UsernamePasswordAuthenticationToken proceedToken(String token) {
        Claims claims = jwtService.extractAllClaims(token);
        String subject = claims.getSubject();

        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);
        @SuppressWarnings("unchecked")
        List<String> authorities = claims.get("authorities", List.class);

        List<GrantedAuthority> grantedAuthorities = new ArrayList<>();
        if (roles != null && roles.size() > 0)
            roles.forEach(role -> grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_" + role)));
        if (authorities != null && authorities.size() > 0)
        authorities.forEach(authority -> grantedAuthorities.add(new SimpleGrantedAuthority(authority)));

        return new UsernamePasswordAuthenticationToken(subject, null, grantedAuthorities);
    }
    
}
