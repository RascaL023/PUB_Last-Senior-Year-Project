package id.my.rascal.search.meilisearch.internal.model.response;

public record MeiliTaskResponse(
    int taskUid,
    String indexUid,
    String status,
    String type
) {}
