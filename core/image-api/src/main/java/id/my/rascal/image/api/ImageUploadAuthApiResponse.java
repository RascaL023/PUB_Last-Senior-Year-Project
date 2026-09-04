package id.my.rascal.image.api;

public record ImageUploadAuthApiResponse(
    String publicKey,
    String token,
    long expire,
    String signature
) {}
