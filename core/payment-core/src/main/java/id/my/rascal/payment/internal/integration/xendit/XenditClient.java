package id.my.rascal.payment.internal.integration.xendit;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Thin client for the Xendit Invoice API (v2). Kept deliberately small and
 * Xendit-specific; no multi-provider abstraction. Uses the project's
 * {@link RestClient} convention (see ImageKitImageService).
 *
 * <p>Authentication follows the official Xendit Invoice API: HTTP Basic auth
 * where the username is the API key and the password is empty. The private key
 * is never logged or returned.
 */
@Component
public class XenditClient {

    private static final Logger log = LoggerFactory.getLogger(XenditClient.class);

    private final RestClient client;

    @Autowired
    public XenditClient(XenditProperties props) {
        this.client = buildClient(props);
    }

    // Package-private for tests that inject a mocked request factory.
    XenditClient(RestClient client) {
        this.client = client;
    }

    public XenditInvoiceResponse createInvoice(XenditInvoiceRequest request) {
        try {
            return client.post()
                    .uri("/v2/invoices")
                    .accept(MediaType.APPLICATION_JSON)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(XenditInvoiceResponse.class);
        } catch (RestClientResponseException ex) {
            String errorBody = ex.getResponseBodyAsString();
            log.error("Xendit invoice creation failed: status={}, body={}", ex.getStatusCode(), errorBody);
            throw new XenditClientException(
                    "Xendit invoice creation failed with status " + ex.getStatusCode(), ex);
        }
    }

    private static RestClient buildClient(XenditProperties props) {
        String basicAuth = "Basic " + Base64.getEncoder()
                .encodeToString((props.privateKey() + ":").getBytes(StandardCharsets.UTF_8));
        return RestClient.builder()
                .baseUrl(props.baseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, basicAuth)
                .build();
    }
}
