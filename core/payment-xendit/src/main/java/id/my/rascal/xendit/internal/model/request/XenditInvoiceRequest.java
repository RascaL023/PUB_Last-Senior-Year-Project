package id.my.rascal.xendit.internal.model.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record XenditInvoiceRequest(
    @JsonProperty("external_id") String externalId,
    @JsonProperty("amount") Integer amount,
    @JsonProperty("currency") String currency,
    @JsonProperty("description") String description,
    @JsonProperty("success_redirect_url") String successRedirectUrl,
    @JsonProperty("failure_redirect_url") String failureRedirectUrl
) {}
