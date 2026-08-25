package id.rascal.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class HeaderAuthFilter extends OncePerRequestFilter {

    private void debug(HttpServletRequest request) {
        Enumeration<String> headers = request.getHeaderNames();
        while (headers.hasMoreElements()) { 
            String header = headers.nextElement();
            System.out.println(header + " = " + request.getHeader(header));
        }
    }

    private void printList(String name, List<String> list) {
        System.out.println(name);
        list.forEach(v -> System.out.println(v));
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request, 
        HttpServletResponse response, 
        FilterChain filterChain
    ) throws ServletException, IOException {
        debug(request);

        String userId = request.getHeader("X-User-Id");
        String signature = request.getHeader("X-Internal-Signature");
        if (
            (signature != null && signature.equals("atlanta331")) &&
            (userId != null && !userId.isBlank())
        ) {
            List<GrantedAuthority> grantedAuthorities = new ArrayList<>();

            String headerTemp = request.getHeader("X-User-Roles");
            List<String> roles = (headerTemp != null && !headerTemp.isBlank()) ?
                List.of(headerTemp.split(",")) :
                List.of();

            headerTemp = request.getHeader("X-User-Authorities");
            List<String> authorities = (headerTemp != null && !headerTemp.isBlank()) ?
                List.of(headerTemp.split(",")) :
                List.of();
            
            roles.forEach(role -> grantedAuthorities.add(
                new SimpleGrantedAuthority("ROLE_" + role.trim().toUpperCase()))
            );
            authorities.forEach(authority -> grantedAuthorities.add(
                new SimpleGrantedAuthority(authority.trim()))
            );

            printList("Roles:", roles);
            printList("Authorities:", authorities);
            SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, grantedAuthorities)
            );
        }

        filterChain.doFilter(request, response);
    }
    
}
