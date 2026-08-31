package id.my.rascal.payment.internal.integration.xendit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class XenditProperties {

    private final String privateKey;
    private final String publicKey;
    private final String callbackToken;
    private final String baseUrl;

    public XenditProperties(
        @Value("${xendit.private-key:}") String privateKey,
        @Value("${xendit.public-key:}") String publicKey,
        @Value("${xendit.callback-token:}") String callbackToken,
        @Value("${xendit.base-url:https://api.xendit.co}") String baseUrl
    ) {
        this.privateKey = privateKey;
        this.publicKey = publicKey;
        this.callbackToken = callbackToken;
        this.baseUrl = baseUrl;
    }

    public String privateKey() {
        return privateKey;
    }

    public String publicKey() {
        return publicKey;
    }

    public String callbackToken() {
        return callbackToken;
    }

    public String baseUrl() {
        return baseUrl;
    }
}
