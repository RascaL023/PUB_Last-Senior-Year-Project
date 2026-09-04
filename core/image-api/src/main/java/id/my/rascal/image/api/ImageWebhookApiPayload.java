package id.my.rascal.image.api;

public record ImageWebhookApiPayload(
    String eventType,
    String fileId,
    String filePath,
    String url
) {}
