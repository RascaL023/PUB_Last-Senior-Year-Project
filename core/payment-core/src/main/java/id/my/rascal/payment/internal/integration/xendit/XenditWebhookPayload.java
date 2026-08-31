package id.my.rascal.payment.internal.integration.xendit;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record XenditWebhookPayload(
    @JsonProperty("id") String id,
    @JsonProperty("external_id") String externalId,
    @JsonProperty("status") String status,
    @JsonProperty("amount") Integer amount,
    @JsonProperty("paid_amount") Integer paidAmount,
    @JsonProperty("payment_method") String paymentMethod,
    @JsonProperty("payment_channel") String paymentChannel,
    @JsonProperty("currency") String currency,
    @JsonProperty("bank_code") String bankCode,
    @JsonProperty("payer_email") String payerEmail,
    @JsonProperty("description") String description,
    @JsonProperty("adjusted_received_amount") Integer adjustedReceivedAmount,
    @JsonProperty("fees_paid_amount") Integer feesPaidAmount,
    @JsonProperty("payment_destination") String paymentDestination,
    @JsonProperty("user_id") String userId,
    @JsonProperty("is_high") Boolean isHigh,
    @JsonProperty("created") String created,
    @JsonProperty("updated") String updated,
    @JsonProperty("merchant_name") String merchantName,
    @JsonProperty("payment_id") String paymentId,
    @JsonProperty("payment_method_id") String paymentMethodId,
    @JsonProperty("payment_details") Object paymentDetails
) {}
