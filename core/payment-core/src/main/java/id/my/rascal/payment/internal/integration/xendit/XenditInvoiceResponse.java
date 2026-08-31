package id.my.rascal.payment.internal.integration.xendit;

import com.fasterxml.jackson.annotation.JsonProperty;

public record XenditInvoiceResponse(
    @JsonProperty("id") String id,
    @JsonProperty("external_id") String externalId,
    @JsonProperty("invoice_url") String invoiceUrl,
    @JsonProperty("status") String status,
    @JsonProperty("amount") Integer amount
) {}
