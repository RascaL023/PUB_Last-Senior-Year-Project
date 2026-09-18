package id.my.rascal.invoice.internal.model.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateInvoiceRequest(
    @Min(value = 1, message = "Invalid dining ID")
    Long diningId,

    @Min(value = 1, message = "Invalid customer ID")
    Long customerId,

    String customerName,

    @NotEmpty(message = "Invoice must have at least 1 item")
    List<@Valid InvoiceItemRequest> items
) {}
