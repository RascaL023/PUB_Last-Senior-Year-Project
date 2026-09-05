package id.my.rascal.search.meilisearch.internal.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.client.RestClient;

@Validated
@Configuration
@EnableConfigurationProperties(MeilisearchProperties.class)
public class MeilisearchConfiguration {

    private final static int CONNECTION_TIME_OUT = 2;
    private final static int READ_TIME_OUT = 5;

    @Bean
    public RestClient meilisearchRestClient(MeilisearchProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(CONNECTION_TIME_OUT))
            .build();

        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(READ_TIME_OUT));

        return RestClient.builder()
            .baseUrl(properties.url())
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
            .requestFactory(requestFactory)
            .build();
    }

}
