package id.my.rascal.search.meilisearch.internal.config;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import id.my.rascal.search.api.IndexSettings;
import id.my.rascal.search.api.SearchIndexInitializer;
import id.my.rascal.search.meilisearch.internal.model.request.MeiliSettingsRequest;
import id.my.rascal.search.meilisearch.internal.model.response.MeiliErrorResponse;

@Component
public class MeilisearchIndexBootstrap {

    private static final Logger log = LoggerFactory.getLogger(MeilisearchIndexBootstrap.class);
    private final List<SearchIndexInitializer> initializers;
    private final RestClient client;

    public MeilisearchIndexBootstrap(RestClient client, List<SearchIndexInitializer> initializers) {
        this.client = client;
        this.initializers = initializers;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        for (SearchIndexInitializer initializer : initializers) applySettings(initializer);
    }

    private void applySettings(SearchIndexInitializer initializer) {
        String indexName = initializer.indexName();
        try {
            IndexSettings declared = initializer.indexSettings();
            ensureIndexExists(indexName);

            MeiliSettingsRequest settings = new MeiliSettingsRequest(
                declared.searchableAttributes(),
                declared.filterableAttributes(),
                declared.sortableAttributes());

            client.patch()
                .uri("/indexes/{index}/settings", indexName)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(settings)
                .retrieve()
                .toBodilessEntity();

            log.info("Applied Meilisearch settings for index '{}': searchable={}, filterable={}, sortable={}",
                indexName,
                declared.searchableAttributes(),
                declared.filterableAttributes(),
                declared.sortableAttributes());
        } catch (ResourceAccessException e) {
            log.error("Meilisearch unreachable, settings for index '{}' not applied: {}", indexName, e.getMessage());
        } catch (Exception e) {
            log.error("Failed to apply Meilisearch settings for index '{}': {}", indexName, e.getMessage(), e);
        }
    }

    private void ensureIndexExists(String indexName) {
        boolean exists;
        try {
            client.get()
                .uri("/indexes/{index}", indexName)
                .retrieve()
                .toBodilessEntity();
            exists = true;
        } catch (RestClientResponseException e) {
            MeiliErrorResponse error = MeilisearchJson.parseError(e);
            if (error != null && "index_not_found".equals(error.code())) exists = false;
            else throw e;
        }

        if (exists) return;
        try {
            client.post()
                .uri("/indexes")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(Map.of("uid", indexName))
                .retrieve()
                .toBodilessEntity();
        } catch (RestClientResponseException e) {
            MeiliErrorResponse error = MeilisearchJson.parseError(e);
            if (error == null || !"index_already_exists".equals(error.code()))
                throw e;
        }
    }

}
