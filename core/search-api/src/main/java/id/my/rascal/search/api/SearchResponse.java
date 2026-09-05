package id.my.rascal.search.api;

import java.util.List;

public record SearchResponse<T>(
    List<SearchHit<T>> hits,
    long totalHits,
    int page,
    int pageSize
) {}
