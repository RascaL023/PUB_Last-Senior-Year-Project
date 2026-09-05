package id.my.rascal.search.api;

import java.util.List;

public record IndexSettings(
    List<String> searchableAttributes,
    List<String> filterableAttributes,
    List<String> sortableAttributes
) {

    public static IndexSettings of(
        List<String> searchableAttributes,
        List<String> filterableAttributes,
        List<String> sortableAttributes
    ) {
        return new IndexSettings(searchableAttributes, filterableAttributes, sortableAttributes);
    }
}
