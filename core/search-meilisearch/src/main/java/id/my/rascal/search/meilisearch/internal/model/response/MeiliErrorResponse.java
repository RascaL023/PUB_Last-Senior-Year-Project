package id.my.rascal.search.meilisearch.internal.model.response;

public record MeiliErrorResponse(
    String message,
    String code,
    String type,
    String link
) {}
