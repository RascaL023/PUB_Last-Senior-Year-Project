package id.my.rascal.image.api;

public interface ImageRegistryApi {

    String registerOrUpdate(String fileId, String filePath);
    String resolveAndDelete(String fileId);

}
