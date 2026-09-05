package id.my.rascal.search.api;

public record SearchHit<T>(
    T document,
    double score
) {}
