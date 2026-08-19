package id.my.rascal.common.image;

/**
 * Storage abstraction for images. DB only stores a relative path like
 * {@code /assets/images/menus/<category_code>/nama_file}; implementations are
 * responsible for resolving that path to a public URL and for deleting the
 * underlying file when needed. Swap the implementation to change the CDN/infra
 * without touching domain code.
 */
public interface ImageService {

    /** Build a storage path under {@code folderPath} with a sanitized file name. */
    String buildPath(String folderPath, String fileName);

    /** Resolve a stored relative path to a public, absolute URL. */
    String resolveUrl(String path);

    /** Permanently delete the file identified by a stored relative path. */
    void deleteByPath(String path);

    /**
     * Generate a short-lived, signed credential the frontend can use to upload
     * a file directly to the image service (keeps the private key server-side).
     */
    ImageUploadAuth generateUploadAuth();
}
