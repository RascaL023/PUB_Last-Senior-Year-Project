package id.my.rascal.image.api;

public interface ImageApi {

    String buildPath(String folderPath, String fileName);
    String resolveUrl(String path);
    void deleteByPath(String path);
    ImageUploadAuthApiResponse getAuthenticationParameters();

}
