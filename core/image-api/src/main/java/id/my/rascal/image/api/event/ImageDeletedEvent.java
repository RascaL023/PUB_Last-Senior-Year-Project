package id.my.rascal.image.api.event;

public record ImageDeletedEvent(String filePath, String fileId, String url) {}
