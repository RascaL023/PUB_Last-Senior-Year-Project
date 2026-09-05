package id.my.rascal.search.api;

import java.util.List;

public record SearchApiRequest(
    String query,
    List<String> filter,
    List<String> sort,
    int page,
    int pageSize
) {
    public static SearchApiRequest of(String query, int page, int pageSize) {
        return new SearchApiRequest(query, List.of(), List.of(), page, pageSize);
    }
}
