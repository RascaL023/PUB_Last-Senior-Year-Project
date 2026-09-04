package id.my.rascal.image.api.event;

public record ImageUpdatedEvent(String filePath, String previousFilePath, String fileId, String url) {}
