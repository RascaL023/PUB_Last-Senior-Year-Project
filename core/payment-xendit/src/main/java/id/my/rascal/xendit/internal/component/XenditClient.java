package id.my.rascal.xendit.internal.component;

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

import id.my.rascal.xendit.internal.exception.XenditClientException;
import id.my.rascal.xendit.internal.model.request.XenditInvoiceRequest;
import id.my.rascal.xendit.internal.model.response.XenditInvoiceResponse;

@Component
public class XenditClient {

    private static final String INVOIICE_ENDPOINT = "/v2/invoices";
    private static final Logger log = LoggerFactory.getLogger(XenditClient.class);
    private final RestClient client;

    @Autowired
    public XenditClient(XenditProperties props) {
        this.client = buildClient(props);
    }

    public XenditInvoiceResponse createInvoice(XenditInvoiceRequest request) {
        try {
            return client.post()
                .uri(INVOIICE_ENDPOINT)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(XenditInvoiceResponse.class);
        } catch (RestClientResponseException ex) {
            String errorBody = ex.getResponseBodyAsString();
            log.error("Xendit invoice creation failed: status={}, body={}", ex.getStatusCode(), errorBody);
            throw new XenditClientException("Xendit invoice creation failed with status " + ex.getStatusCode(), ex);
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
