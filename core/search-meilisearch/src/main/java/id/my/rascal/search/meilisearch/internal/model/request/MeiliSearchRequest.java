package id.my.rascal.search.meilisearch.internal.model.request;

import java.util.List;

public record MeiliSearchRequest(
    String q,
    List<String> filter,
    List<String> sort,
    Integer page,
    Integer hitsPerPage,
    Boolean showRankingScore
) {}
