package id.my.rascal.invoice.internal.model.mapper;

import id.my.rascal.invoice.api.InvoiceApiResponse;
import id.my.rascal.invoice.api.InvoiceItemApiResponse;
import id.my.rascal.invoice.internal.entity.Invoice;
import id.my.rascal.invoice.internal.entity.InvoiceItem;
import id.my.rascal.invoice.internal.model.response.InvoiceItemResponse;
import id.my.rascal.invoice.internal.model.response.InvoiceResponse;

import java.util.List;

public class InvoiceMapper {

    private InvoiceMapper() { }

    public static InvoiceApiResponse toApiResponse(Invoice invoice) {
        return new InvoiceApiResponse(
            invoice.getId(),
            invoice.getInvoiceNumber(),
            invoice.getStatus().name(),
            invoice.getTotalAmount(),
            invoice.getPaidAmount(),
            invoice.getRemainingAmount(),
            invoice.getIssuedAt(),
            invoice.getCreatedAt(),
            invoice.getItems().stream().map(InvoiceMapper::toItemApiResponse).toList()
        );
    }

    public static InvoiceApiResponse toApiResponse(InvoiceResponse response) {
        return new InvoiceApiResponse(
            response.id(),
            response.invoiceNumber(),
            response.status().name(),
            response.totalAmount(),
            response.paidAmount(),
            response.remainingAmount(),
            response.issuedAt(),
            response.createdAt(),
            response.items().stream().map(InvoiceMapper::toItemApiResponse).toList()
        );
    }

    public static InvoiceResponse toResponse(Invoice invoice) {
        List<InvoiceItemResponse> items = invoice.getItems().stream()
            .map(InvoiceMapper::toItemResponse)
            .toList();

        return new InvoiceResponse(
            invoice.getId(),
            invoice.getInvoiceNumber(),
            invoice.getStatus(),
            invoice.getTotalAmount(),
            invoice.getPaidAmount(),
            invoice.getRemainingAmount(),
            invoice.getIssuedAt(),
            invoice.getCreatedAt(),
            invoice.getUpdatedAt(),
            items
        );
    }

    public static InvoiceItemResponse toItemResponse(InvoiceItem item) {
        return new InvoiceItemResponse(
            item.getId(),
            item.getOrderId(),
            item.getDescription(),
            item.getQuantity(),
            item.getUnitPrice(),
            item.getAmount()
        );
    }

    public static InvoiceItemApiResponse toItemApiResponse(InvoiceItem item) {
        return new InvoiceItemApiResponse(
            item.getId(),
            item.getOrderId(),
            item.getDescription(),
            item.getQuantity(),
            item.getUnitPrice(),
            item.getAmount()
        );
    }

    public static InvoiceItemApiResponse toItemApiResponse(InvoiceItemResponse response) {
        return new InvoiceItemApiResponse(
            response.id(),
            response.orderId(),
            response.description(),
            response.quantity(),
            response.unitPrice(),
            response.amount()
        );
    }

}
