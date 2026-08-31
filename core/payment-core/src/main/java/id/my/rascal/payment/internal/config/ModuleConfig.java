package id.my.rascal.payment.internal.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class ModuleConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
    
}
