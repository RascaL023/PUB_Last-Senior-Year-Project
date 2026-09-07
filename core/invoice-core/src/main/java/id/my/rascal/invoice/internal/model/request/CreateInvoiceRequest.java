package id.my.rascal.invoice.internal.model.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateInvoiceRequest(
    @NotEmpty(message = "Invoice must have at least 1 item")
    List<@Valid InvoiceItemRequest> items
) {}
