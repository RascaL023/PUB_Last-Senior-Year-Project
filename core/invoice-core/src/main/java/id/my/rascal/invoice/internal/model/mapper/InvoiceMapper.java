package id.my.rascal.invoice.internal.model.mapper;

import id.my.rascal.invoice.api.InvoiceApiResponse;
import id.my.rascal.invoice.api.InvoiceItemApiResponse;
import id.my.rascal.invoice.internal.entity.Invoice;
import id.my.rascal.invoice.internal.entity.InvoiceItem;
import id.my.rascal.invoice.internal.model.request.InvoiceItemRequest;
import id.my.rascal.invoice.internal.model.response.InvoiceItemResponse;
import id.my.rascal.invoice.internal.model.response.InvoiceResponse;

import java.time.LocalDateTime;
import java.util.List;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.common.util.StringUtil;

public class InvoiceMapper {

    private InvoiceMapper() { }

    public static InvoiceItem toItemEntity(Invoice invoice, InvoiceItemRequest request) {
        if (request.orderItemId() == null || request.orderItemId() < 1)
            throw new BadRequestException("Order item ID is required");
        if (request.quantity() == null || request.quantity() < 1)
            throw new BadRequestException("Quantity must be at least 1");
        if (request.unitPrice() == null || request.unitPrice() < 0)
            throw new BadRequestException("Unit price cannot be negative");
        if (request.amount() == null || request.amount() < 0)
            throw new BadRequestException("Amount cannot be negative");

        InvoiceItem item = new InvoiceItem();
        item.setInvoice(invoice);
        item.setOrderItemId(request.orderItemId());
        item.setOrderId(request.orderId());
        item.setDescription(StringUtil.normalizeSpaces(request.description()));
        item.setQuantity(request.quantity());
        item.setUnitPrice(request.unitPrice());
        item.setAmount(request.amount());
        item.setCreatedAt(LocalDateTime.now());
        return item;
    }

    public static InvoiceApiResponse toApiResponse(Invoice invoice) {
        return new InvoiceApiResponse(
            invoice.getId(),
            invoice.getInvoiceNumber(),
            invoice.getDiningId(),
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
            response.diningId(),
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
            invoice.getDiningId(),
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
            item.getOrderItemId(),
            item.getOrderId(),
            item.getDescription(),
            item.getQuantity(),
            item.getUnitPrice(),
            item.getAmount(),
            item.getRefunded()
        );
    }

    public static InvoiceItemApiResponse toItemApiResponse(InvoiceItem item) {
        return new InvoiceItemApiResponse(
            item.getId(),
            item.getOrderItemId(),
            item.getOrderId(),
            item.getDescription(),
            item.getQuantity(),
            item.getUnitPrice(),
            item.getAmount(),
            item.getRefunded()
        );
    }

    public static InvoiceItemApiResponse toItemApiResponse(InvoiceItemResponse response) {
        return new InvoiceItemApiResponse(
            response.id(),
            response.orderItemId(),
            response.orderId(),
            response.description(),
            response.quantity(),
            response.unitPrice(),
            response.amount(),
            response.refunded()
        );
    }

}
