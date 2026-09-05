package id.my.rascal.search.meilisearch.internal.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.springframework.web.client.RestClientResponseException;

import id.my.rascal.search.meilisearch.internal.model.response.MeiliErrorResponse;

public final class MeilisearchJson {

    private static final ObjectMapper MAPPER = JsonMapper.builder()
        .addModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .serializationInclusion(JsonInclude.Include.NON_NULL)
        .build();

    private MeilisearchJson() {}

    public static ObjectMapper mapper() { return MAPPER; }

    public static MeiliErrorResponse parseError(RestClientResponseException e) {
        try {
            return MAPPER.readValue(e.getResponseBodyAsString(), MeiliErrorResponse.class);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

}
