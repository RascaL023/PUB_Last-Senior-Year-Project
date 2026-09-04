package id.my.rascal.imagekit.internal.component;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ImageKitProperties {

    private final String baseUrl;
    private final String urlEndpoint;
    private final String publicKey;
    private final String privateKey;
    private final String webhooksEndpoint;
    private final String webhooksSecret;

    public ImageKitProperties(
        @Value("${imagekit.base-url:}") String baseUrl,
        @Value("${imagekit.url-endpoint:}") String urlEndpoint,
        @Value("${imagekit.public-key:}") String publicKey,
        @Value("${imagekit.private-key:}") String privateKey,
        @Value("${imagekit.webhooks.endpoint:}") String webhooksEndpoint,
        @Value("${imagekit.webhooks.secret:}") String webhooksSecret
    ) {
        this.baseUrl = baseUrl;
        this.urlEndpoint = urlEndpoint;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.webhooksEndpoint = webhooksEndpoint;
        this.webhooksSecret = webhooksSecret;
    }

    public String baseUrl() {
        return baseUrl;
    }

    public String urlEndpoint() {
        return urlEndpoint;
    }

    public String publicKey() {
        return publicKey;
    }

    public String privateKey() {
        return privateKey;
    }

    public String webhooksEndpoint() {
        return webhooksEndpoint;
    }

    public String webhooksSecret() {
        return webhooksSecret;
    }

}
