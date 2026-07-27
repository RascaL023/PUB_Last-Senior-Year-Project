package id.my.rascal.common.template;

public record PaginationTemplate(
    int currentPage,
    int perPage,
    long totalItems,
    int totalPages,
    boolean hasNextPage,
    boolean hasPrevPage
) { }
