package id.my.rascal.invoice.internal.service;

import id.my.rascal.common.exception.NotFoundException;
import id.my.rascal.invoice.internal.entity.Invoice;
import id.my.rascal.invoice.internal.entity.InvoiceItem;
import id.my.rascal.invoice.internal.model.mapper.InvoiceMapper;
import id.my.rascal.invoice.internal.model.request.ApplyPaymentRequest;
import id.my.rascal.invoice.internal.model.request.CreateInvoiceRequest;
import id.my.rascal.invoice.internal.model.request.InvoiceItemRequest;
import id.my.rascal.invoice.internal.model.response.InvoiceResponse;
import id.my.rascal.invoice.internal.repository.InvoiceRepository;
import id.my.rascal.invoice.internal.util.InvoiceNumberGenerator;
import id.my.rascal.dining.api.event.DiningOrderAddedEvent;
import id.my.rascal.order.api.event.OrderCancelledEvent;
import id.my.rascal.order.api.event.StandaloneOrderCreatedEvent;
import id.my.rascal.order.api.event.dto.OrderItemSnapshot;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;

    public InvoiceService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @Transactional
    public InvoiceResponse create(CreateInvoiceRequest request) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(InvoiceNumberGenerator.generateUniqueInvoiceNumber(invoiceRepository::existsByInvoiceNumber));
        invoice.setDiningId(request.diningId());

        List<InvoiceItem> items = request.items().stream()
            .map(itemRequest -> InvoiceMapper.toItemEntity(invoice, itemRequest))
            .toList();
        invoice.setItems(items);
        invoice.setTotalAmount(items.stream().mapToInt(InvoiceItem::getAmount).sum());
        invoice.setPaidAmount(0);
        invoice.setRemainingAmount(invoice.getTotalAmount());

        LocalDateTime now = LocalDateTime.now();
        invoice.setIssuedAt(now);
        invoice.setCreatedAt(now);
        invoice.markOpen();

        return InvoiceMapper.toResponse(invoiceRepository.save(invoice));
    }

    @Transactional
    public InvoiceResponse handleStandaloneOrderCreated(StandaloneOrderCreatedEvent event) {
        List<InvoiceItemRequest> freshItems = event.items().stream()
            .filter(item -> !invoiceRepository.existsByItemsOrderItemId(item.orderItemId()))
            .map(item -> toItemRequest(event.orderId(), item))
            .toList();
        if (freshItems.isEmpty())
            return null;

        return create(new CreateInvoiceRequest(null, freshItems));
    }

    @Transactional
    public InvoiceResponse handleOrderAddedToDining(DiningOrderAddedEvent event) {
        Invoice invoice = invoiceRepository.findActiveByDiningId(event.diningId())
            .orElseGet(() -> initDiningInvoice(event.diningId()));

        Set<Long> billedItemIds = invoice.getItems().stream()
            .map(InvoiceItem::getOrderItemId)
            .collect(Collectors.toSet());

        List<InvoiceItem> freshItems = event.items().stream()
            .filter(item -> !billedItemIds.contains(item.orderItemId()))
            .map(item -> InvoiceMapper.toItemEntity(invoice, toItemRequest(event.orderId(), item)))
            .toList();
        if (!freshItems.isEmpty()) {
            invoice.getItems().addAll(freshItems);
            invoice.setTotalAmount(invoice.getItems().stream().mapToInt(InvoiceItem::getAmount).sum());
            invoice.setRemainingAmount(invoice.getTotalAmount() - invoice.getPaidAmount());
            invoice.setUpdatedAt(LocalDateTime.now());
        }

        return InvoiceMapper.toResponse(invoiceRepository.save(invoice));
    }

    @Transactional
    public void handleOrderCancelled(OrderCancelledEvent event) {
        invoiceRepository.findActiveByItemsOrderId(event.orderId()).stream()
            .filter(i -> i.getDiningId() == null)
            .filter(i -> i.getPaidAmount() == 0)
            .forEach(i -> {
                i.voidInvoice();
                i.setUpdatedAt(LocalDateTime.now());
                invoiceRepository.save(i);
            });
    }

    @Transactional
    public InvoiceResponse applyPayment(Long id, ApplyPaymentRequest request) {
        Invoice invoice = findActiveInvoice(id);
        invoice.applyPayment(request.amount());
        return InvoiceMapper.toResponse(invoiceRepository.save(invoice));
    }

    @Transactional
    public InvoiceResponse applyPayment(Long id, Integer amount) {
        return applyPayment(id, new ApplyPaymentRequest(amount));
    }

    @Transactional
    public InvoiceResponse voidInvoice(Long id) {
        Invoice invoice = findActiveInvoice(id);
        invoice.voidInvoice();
        return InvoiceMapper.toResponse(invoiceRepository.save(invoice));
    }

    @Transactional
    public void delete(Long id) {
        Invoice invoice = findActiveInvoice(id);
        invoice.setDeletedAt(LocalDateTime.now());
        invoiceRepository.save(invoice);
    }


    private Invoice findActiveInvoice(Long id) {
        return invoiceRepository.findActiveById(id)
            .orElseThrow(() -> new NotFoundException("Invoice not found with id: " + id));
    }

    private Invoice initDiningInvoice(Long diningId) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(InvoiceNumberGenerator.generateUniqueInvoiceNumber(invoiceRepository::existsByInvoiceNumber));
        invoice.setDiningId(diningId);
        invoice.setTotalAmount(0);
        invoice.setPaidAmount(0);
        invoice.setRemainingAmount(0);

        LocalDateTime now = LocalDateTime.now();
        invoice.setIssuedAt(now);
        invoice.setCreatedAt(now);
        invoice.markOpen();

        return invoice;
    }

    private InvoiceItemRequest toItemRequest(Long orderId, OrderItemSnapshot snapshot) {
        return new InvoiceItemRequest(
            snapshot.orderItemId(),
            orderId,
            snapshot.itemName(),
            snapshot.quantity(),
            snapshot.unitPrice(),
            snapshot.subtotal()
        );
    }

}
