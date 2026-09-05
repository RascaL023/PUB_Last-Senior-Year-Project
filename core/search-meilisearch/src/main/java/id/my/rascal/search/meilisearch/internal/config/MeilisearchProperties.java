package id.my.rascal.search.meilisearch.internal.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "meilisearch")
public record MeilisearchProperties(
    @NotBlank String url,
    @NotBlank String apiKey
) {}
