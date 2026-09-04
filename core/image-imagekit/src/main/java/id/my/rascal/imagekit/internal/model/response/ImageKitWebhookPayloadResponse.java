package id.my.rascal.imagekit.internal.model.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ImageKitWebhookPayloadResponse(
    @JsonProperty("type") String type,
    @JsonProperty("id") String id,
    @JsonProperty("created_at") String createdAt,
    @JsonProperty("data") ImageKitWebhookDataResponse data
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ImageKitWebhookDataResponse(
        @JsonAlias({"fileId", "file_id"}) String fileId,
        @JsonAlias({"filePath", "file_path"}) String filePath,
        @JsonProperty("url") String url
    ) {}
}
