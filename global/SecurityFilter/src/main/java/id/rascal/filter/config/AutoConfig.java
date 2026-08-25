package id.rascal.filter.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;

import id.rascal.filter.HeaderAuthFilter;
import id.rascal.filter.JwtAuthFilter;
import id.rascal.filter.inteface.KeyProvider;
import id.rascal.filter.service.JwtService;
import tools.jackson.databind.ObjectMapper;

@AutoConfiguration
public class AutoConfig {

    @Bean
    @ConditionalOnClass(name = "io.jsonwebtoken.Jwts")
    public KeyProvider keyProvider() {
        return new KeyProvider() {}; // default implementation
    }

    @Bean
    @ConditionalOnClass(name = "io.jsonwebtoken.Jwts")
    public JwtService jwtService(KeyProvider keyProvider) {
        return new JwtService(keyProvider);
    }

    @Bean
    @ConditionalOnClass(name = "io.jsonwebtoken.Jwts")
    public JwtAuthFilter jwtAuthFilter(ObjectMapper mapper, JwtService jwtService) {
        return new JwtAuthFilter(mapper, jwtService);
    }

    @Bean
    public HeaderAuthFilter headerAuthFilter() {
        return new HeaderAuthFilter();
    }

}
