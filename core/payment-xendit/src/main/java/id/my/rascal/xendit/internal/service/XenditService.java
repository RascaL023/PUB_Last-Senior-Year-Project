package id.my.rascal.xendit.internal.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.payment.api.PaymentApi;
import id.my.rascal.payment.api.PaymentApiWebhookRequest;
import id.my.rascal.payment.api.PaymentProcessorRequest;
import id.my.rascal.payment.api.PaymentProcessorResponse;
import id.my.rascal.payment.api.PaymentProcessorStatus;
import id.my.rascal.xendit.internal.component.XenditClient;
import id.my.rascal.xendit.internal.component.XenditProperties;
import id.my.rascal.xendit.internal.exception.XenditClientException;
import id.my.rascal.xendit.internal.model.request.XenditInvoiceRequest;
import id.my.rascal.xendit.internal.model.response.XenditInvoiceResponse;
import id.my.rascal.xendit.internal.model.response.XenditWebhookPayloadResponse;

@Service
public class XenditService {

    private final XenditProperties xenditProperties;
    private final XenditClient xenditClient;
    private final ObjectMapper objectMapper;
    private final PaymentApi paymentApi;
    private static final Logger logger = LoggerFactory.getLogger(XenditService.class);

    public XenditService(
        XenditProperties xenditProperties,
        XenditClient xenditClient,
        PaymentApi paymentApi,
        ObjectMapper objectMapper
    ) {
        this.xenditProperties = xenditProperties;
        this.xenditClient = xenditClient;
        this.paymentApi = paymentApi;
        this.objectMapper = objectMapper;
    }

    public boolean isValidToken(String rawCallbackToken) {
        if (rawCallbackToken == null || !rawCallbackToken.equals(xenditProperties.callbackToken())) {
            logger.warn("Invalid callback token: {}", rawCallbackToken);
            return false;
        }

        return true;
    }

    public void handleWebhook(String rawPayload) {
        try {
            XenditWebhookPayloadResponse payload = objectMapper.readValue(rawPayload, XenditWebhookPayloadResponse.class);
            paymentApi.handleWeebhookRequest(toWebhookRequest(payload), rawPayload);
        } catch (JsonProcessingException ex) {
            logger.error("Xendit payload process error: {}", ex.getMessage());
            throw new BadRequestException(null);
        }
    }

    public PaymentProcessorResponse initPayment(PaymentProcessorRequest request) {
        XenditInvoiceResponse invoice = xenditClient.createInvoice(
            new XenditInvoiceRequest(
                request.externalId(), 
                request.amount(), 
                "IDR", 
                request.description(), 
                null, null
            )
        );

        return toProcessorResponse(invoice);
    }


    private PaymentProcessorResponse toProcessorResponse(XenditInvoiceResponse invoiceResponse) {
        try {
            return new PaymentProcessorResponse(
                null, 
                invoiceResponse.externalId(), 
                invoiceResponse.invoiceUrl(), 
                null, null,
                PaymentProcessorStatus.PENDING, 
                invoiceResponse.amount()
            );
        } catch (XenditClientException ex) {
            throw new XenditClientException("Failed to initialize Xendit payment: " + ex.getMessage());
        }
    }

    private PaymentApiWebhookRequest toWebhookRequest(XenditWebhookPayloadResponse payload) {
        Integer settledAmount = payload.paidAmount() != null ? payload.paidAmount() : payload.amount();
        return new PaymentApiWebhookRequest(
            payload.externalId(),
            resolveStatus(payload.status().trim()),
            settledAmount,
            payload.paymentMethod(),
            payload.paymentChannel(),
            payload.currency()
        );
    }
    
    private PaymentProcessorStatus resolveStatus(String status) {
        return switch (status.toUpperCase()) {
            case "PAID", "SUCCESS" -> PaymentProcessorStatus.PAID;
            case "EXPIRED" -> PaymentProcessorStatus.EXPIRED;
            case "PENDING" -> PaymentProcessorStatus.PENDING;
            case "REFUNDED" -> PaymentProcessorStatus.REFUNDED;
            default -> PaymentProcessorStatus.FAILED;
        };
    }
}
