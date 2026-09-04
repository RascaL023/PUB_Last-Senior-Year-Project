package id.my.rascal.image.api.event;

public record ImageCreatedEvent(String filePath, String fileId, String url) {}
