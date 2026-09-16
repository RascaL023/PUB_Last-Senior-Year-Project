package id.my.rascal.notification.resend.internal.adapter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import id.my.rascal.notification.api.EmailSendResultApi;
import id.my.rascal.notification.api.EmailSenderApi;
import id.my.rascal.notification.api.NotificationException;
import id.my.rascal.notification.api.SendEmailRequestApi;
import id.my.rascal.notification.resend.internal.config.ResendProperties;

@Component
public class ResendEmailSender implements EmailSenderApi {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailSender.class);
    private static final String EMAIL_ENDPOINT = "/emails";
    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private final RestClient client;
    private final ResendProperties properties;
    private final ObjectMapper mapper;

    public ResendEmailSender(
        @Qualifier("resendRestClient") RestClient client,
        ResendProperties properties,
        ObjectMapper mapper
    ) {
        this.client = client;
        this.properties = properties;
        this.mapper = mapper;
    }

    @Override
    public EmailSendResultApi sendEmail(SendEmailRequestApi request) {
        try {
            String from = buildFrom();
            var body = new HashMap<String, Object>();
            body.put("from", from);
            body.put("to", List.of(request.to()));
            body.put("subject", request.subject());
            if (request.html() != null) body.put("html", request.html());
            if (request.text() != null) body.put("text", request.text());

            String idempotencyKey = request.headers() != null && request.headers().containsKey("Idempotency-Key")
                ? request.headers().get("Idempotency-Key")
                : UUID.randomUUID().toString();

            String jsonBody = mapper.writeValueAsString(body);
            String responseBody = client.post()
                .uri(EMAIL_ENDPOINT)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(IDEMPOTENCY_KEY_HEADER, idempotencyKey)
                .body(jsonBody)
                .retrieve()
                .body(String.class);

            JsonNode node = mapper.readTree(responseBody);
            String emailId = node.has("id") ? node.get("id").asText() : null;
            return new EmailSendResultApi(emailId);
        } catch (RestClientResponseException e) {
            log.error("Resend send failed: status={}, body={}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new NotificationException("Email sending failed with status " + e.getStatusCode(), e);
        } catch (IOException e) {
            log.error("Failed to process Resend email request", e);
            throw new NotificationException("Failed to process email request", e);
        }
    }

    private String buildFrom() {
        String name = properties.senderName();
        if (name != null && !name.isBlank()) {
            return name + " <" + properties.fromEmail() + ">";
        }
        return properties.fromEmail();
    }
}
