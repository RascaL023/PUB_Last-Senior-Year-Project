package id.my.rascal.search.meilisearch.internal.model.request;

import java.util.List;

public record MeiliSettingsRequest(
    List<String> searchableAttributes,
    List<String> filterableAttributes,
    List<String> sortableAttributes
) {}
