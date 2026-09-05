package id.my.rascal.search.meilisearch.internal.model.response;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

public record MeiliSearchResponse(
    List<JsonNode> hits,
    long totalHits,
    int page,
    int hitsPerPage
) {}
