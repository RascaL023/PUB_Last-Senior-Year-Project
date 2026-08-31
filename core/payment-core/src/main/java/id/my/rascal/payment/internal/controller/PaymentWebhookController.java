package id.my.rascal.payment.internal.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import id.my.rascal.payment.internal.integration.xendit.XenditProperties;
import id.my.rascal.payment.internal.integration.xendit.XenditWebhookPayload;
import id.my.rascal.payment.internal.service.PaymentService;

@RestController
@RequestMapping("/api/v1/payments/webhooks")
public class PaymentWebhookController {

    private final PaymentService paymentService;
    private final XenditProperties xenditProperties;
    private final ObjectMapper objectMapper;

    public PaymentWebhookController(
        PaymentService paymentService,
        XenditProperties xenditProperties,
        ObjectMapper objectMapper
    ) {
        this.paymentService = paymentService;
        this.xenditProperties = xenditProperties;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/xendit")
    public ResponseEntity<Void> xendit(
        @RequestHeader(value = "X-Callback-Token", required = false) String callbackToken,
        @RequestBody String rawPayload
    ) {
        System.out.println(callbackToken);
        System.out.println(rawPayload);
        if (callbackToken == null || !callbackToken.equals(xenditProperties.callbackToken())) {
            System.out.println("Invalid callback token!");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            XenditWebhookPayload payload = objectMapper.readValue(rawPayload, XenditWebhookPayload.class);
            paymentService.handleXenditWebhook(payload, rawPayload);
        } catch (JsonProcessingException ex) {
            System.out.println("Xendit payload error: " + ex.getMessage());
            return ResponseEntity.badRequest().build();
        }

        return ResponseEntity.ok().build();
    }
}
