package id.my.rascal.common.image;

/**
 * Credentials the frontend needs to upload a file directly to the image service
 * (client-side upload). Signed server-side so the private key never leaves the
 * backend.
 */
public record ImageUploadAuth(
    String publicKey,
    String token,
    long expire,
    String signature
) { }