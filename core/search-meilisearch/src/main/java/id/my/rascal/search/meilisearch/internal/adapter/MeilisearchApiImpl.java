package id.my.rascal.search.meilisearch.internal.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import id.my.rascal.search.api.SearchApi;
import id.my.rascal.search.api.SearchApiRequest;
import id.my.rascal.search.api.SearchHit;
import id.my.rascal.search.api.SearchResponse;
import id.my.rascal.search.api.SearchUnavailableException;
import id.my.rascal.search.meilisearch.internal.config.MeilisearchJson;
import id.my.rascal.search.meilisearch.internal.model.request.MeiliSearchRequest;
import id.my.rascal.search.meilisearch.internal.model.response.MeiliErrorResponse;
import id.my.rascal.search.meilisearch.internal.model.response.MeiliSearchResponse;
import id.my.rascal.search.meilisearch.internal.model.response.MeiliTaskResponse;

/**
 * Endpoint yang dipakai (Meilisearch 1.53.x):
 * {@code POST /indexes/{index}/search}</li>
 * {@code POST /indexes/{index}/documents} (body = JSON array)</li>
 * {@code DELETE /indexes/{index}/documents/{id}}</li>
 * {@code GET /indexes/{index}/documents/{id}}</li>
 * Request dan response JSON diproses lewat {@link MeilisearchJson} (ObjectMapper
 * Jackson milik modul ini) — tidak memakai converter RestClient, sehingga
 * perilaku serialisasi konsisten (NON_NULL, JavaTimeModule, toleran terhadap
 * field tak dikenal) terlepas dari konfigurasi JSON Spring Boot.
 */
@Component
public class MeilisearchApiImpl implements SearchApi {

    private static final String INFRA_ERROR_PREFIX = "Meilisearch unavailable";
    private final RestClient client;

    public MeilisearchApiImpl(RestClient client) {
        this.client = client;
    }

    @Override
    public <T> SearchResponse<T> search(String index, SearchApiRequest request, Class<T> documentType) {
        try {
            MeiliSearchRequest body = new MeiliSearchRequest(
                request.query() != null ? request.query() : "",
                emptyToNull(request.filter()),
                emptyToNull(request.sort()),
                request.page() > 0 ? request.page() : null,
                request.pageSize() > 0 ? request.pageSize() : null,
                true
            );

            String responseBody = client.post()
                .uri("/indexes/{index}/search", index)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(MeilisearchJson.mapper().writeValueAsString(body))
                .retrieve()
                .body(String.class);

            if (responseBody == null)
                return new SearchResponse<>(List.of(), 0, request.page(), request.pageSize());

            MeiliSearchResponse response = MeilisearchJson.mapper().readValue(responseBody, MeiliSearchResponse.class);
            List<SearchHit<T>> hits = toHits(response, documentType);

            return new SearchResponse<>(hits, response.totalHits(), response.page(), response.hitsPerPage());
        } catch (ResourceAccessException e) {
            throw new SearchUnavailableException(INFRA_ERROR_PREFIX + ": " + e.getMessage(), e);
        } catch (RestClientResponseException e) {
            MeiliErrorResponse error = MeilisearchJson.parseError(e);
            if (error != null && "index_not_found".equals(error.code()))
                return new SearchResponse<>(List.of(), 0, request.page(), request.pageSize());
            throw e;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse Meilisearch search response: " + e.getMessage(), e);
        }
    }

    @Override
    public int index(String index, Object document) {
        try {
            String json = MeilisearchJson.mapper().writeValueAsString(document);
            if (!json.trim().startsWith("["))
                json = "[" + json + "]";

            String responseBody = client.post()
                .uri("/indexes/{index}/documents", index)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(json)
                .retrieve()
                .body(String.class);

            if (responseBody == null) return -1;
            MeiliTaskResponse task = MeilisearchJson.mapper().readValue(responseBody, MeiliTaskResponse.class);

            return task != null ? task.taskUid() : -1;
        } catch (ResourceAccessException e) {
            throw new SearchUnavailableException(INFRA_ERROR_PREFIX + ": " + e.getMessage(), e);
        } catch (RestClientResponseException e) { throw e; } 
        catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize search document: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String index, String documentId) {
        try {
            client.delete()
                .uri("/indexes/{index}/documents/{id}", index, documentId)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .toBodilessEntity();
        } catch (ResourceAccessException e) {
            throw new SearchUnavailableException(INFRA_ERROR_PREFIX + ": " + e.getMessage(), e);
        } catch (RestClientResponseException e) { throw e; }
    }

    @Override
    public <T> Optional<T> getDocument(String index, String documentId, Class<T> documentType) {
        try {
            String responseBody = client.get()
                .uri("/indexes/{index}/documents/{id}", index, documentId)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(String.class);

            if (responseBody == null) return Optional.empty();
            T document = MeilisearchJson.mapper().readValue(responseBody, documentType);

            return Optional.ofNullable(document);
        } catch (ResourceAccessException e) {
            throw new SearchUnavailableException(INFRA_ERROR_PREFIX + ": " + e.getMessage(), e);
        } catch (RestClientResponseException e) {
            MeiliErrorResponse error = MeilisearchJson.parseError(e);
            if (error != null && ("document_not_found".equals(error.code()) || "index_not_found".equals(error.code())))
                return Optional.empty();
            throw e;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to parse Meilisearch document: " + e.getMessage(), e);
        }
    }

    private <T> List<SearchHit<T>> toHits(MeiliSearchResponse response, Class<T> documentType) {
        List<SearchHit<T>> hits = new ArrayList<>();
        if (response.hits() == null) return hits;

        for (JsonNode hit : response.hits()) {
            if (!(hit instanceof ObjectNode node)) continue;

            double score = node.path("_rankingScore").isNumber() ? node.path("_rankingScore").asDouble() : 0.0;
            node.remove("_rankingScore");
            T document = MeilisearchJson.mapper().convertValue(node, documentType);
            hits.add(new SearchHit<>(document, score));
        }

        return hits;
    }

    private static List<String> emptyToNull(List<String> values) {
        return values == null || values.isEmpty() ? null : values;
    }

}
