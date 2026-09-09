package id.my.rascal.invoice.api.event;

public record InvoicePaymentAppliedEvent(
    Long paymentId,
    Long invoiceId,
    Integer appliedAmount,
    Integer excessAmount
) {}
