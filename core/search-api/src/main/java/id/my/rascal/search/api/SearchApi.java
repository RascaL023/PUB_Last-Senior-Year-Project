package id.my.rascal.search.api;

import java.util.Optional;

public interface SearchApi {

    // Or throw
    <T> SearchResponse<T> search(String indexName, SearchApiRequest request, Class<T> documentType);
    int index(String indexName, Object document);
    void delete(String indexName, String documentId);
    <T> Optional<T> getDocument(String indexName, String documentId, Class<T> documentType);

}
