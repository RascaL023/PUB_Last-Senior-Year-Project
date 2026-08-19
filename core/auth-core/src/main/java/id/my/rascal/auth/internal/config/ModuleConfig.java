package id.my.rascal.auth.internal.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import id.my.rascal.auth.internal.exception.SecurityExceptionHandler;
// import id.rascal.filter.HeaderAuthFilter;
import id.rascal.filter.JwtAuthFilter;

@Configuration
@EnableMethodSecurity
public class ModuleConfig {

    // private final HeaderAuthFilter headerAuthFilter;
    private final JwtAuthFilter jwtAuthFilter;
    private final SecurityExceptionHandler securityExceptionHandler;

    public ModuleConfig(
        JwtAuthFilter jwtAuthFilter,
        // HeaderAuthFilter headerAuthFilter,
        SecurityExceptionHandler securityExceptionHandler
    ) {
        // this.headerAuthFilter = headerAuthFilter;
        this.jwtAuthFilter = jwtAuthFilter;
        this.securityExceptionHandler = securityExceptionHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity httpSecurity
    ) throws Exception {
        return httpSecurity
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/v1/auths/login").permitAll()
                .anyRequest().authenticated()
                // .anyRequest().permitAll()
            ).exceptionHandling(ex -> ex
                .authenticationEntryPoint(securityExceptionHandler)
                .accessDeniedHandler(securityExceptionHandler)
            ).addFilterBefore(
                jwtAuthFilter, 
                UsernamePasswordAuthenticationFilter.class
            ).build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

}
