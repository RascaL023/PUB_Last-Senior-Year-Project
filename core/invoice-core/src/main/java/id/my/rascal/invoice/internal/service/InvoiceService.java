package id.my.rascal.invoice.internal.service;

import id.my.rascal.common.exception.NotFoundException;
import id.my.rascal.invoice.internal.entity.Invoice;
import id.my.rascal.invoice.internal.entity.InvoiceItem;
import id.my.rascal.invoice.internal.entity.InvoiceStatus;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class InvoiceService {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceService.class);

    private final InvoiceRepository invoiceRepository;
    private final InvoiceEventPublisherService invoiceEventPublisherService;

    public InvoiceService(
        InvoiceRepository invoiceRepository,
        InvoiceEventPublisherService invoiceEventPublisherService
    ) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceEventPublisherService = invoiceEventPublisherService;
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

        Invoice saved = invoiceRepository.save(invoice);
        invoiceEventPublisherService.publishCreated(saved);
        return InvoiceMapper.toResponse(saved);
    }

    @Transactional
    public InvoiceResponse handleStandaloneOrderCreated(StandaloneOrderCreatedEvent event) {
        // TODO(customer-module): tempelkan snapshot customer (dari event.customerId
        // via customer-api) ke invoice setelah modulnya tersedia.
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
        Optional<Invoice> existing = invoiceRepository.findActiveByDiningId(event.diningId());
        boolean isNew = existing.isEmpty();
        Invoice invoice = existing.orElseGet(() -> initDiningInvoice(event.diningId()));

        if (!isNew && (invoice.getStatus() == InvoiceStatus.VOID || invoice.getStatus() == InvoiceStatus.PAID)) {
            logger.error("Append ditolak: invoice dining sudah final (by-pass guard service?): invoiceId={} diningId={} orderId={} status={}",
                invoice.getId(), event.diningId(), event.orderId(), invoice.getStatus());
            return InvoiceMapper.toResponse(invoice);
        }

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

        Invoice saved = invoiceRepository.save(invoice);
        if (isNew)
            invoiceEventPublisherService.publishCreated(saved);
        return InvoiceMapper.toResponse(saved);
    }

    @Transactional
    public void handleOrderCancelled(OrderCancelledEvent event) {
        invoiceRepository.findActiveByItemsOrderId(event.orderId())
            .forEach(invoice -> {
                if (invoice.getDiningId() == null)
                    voidUnpaidStandaloneInvoice(invoice);
                else
                    removeCancelledDiningItems(invoice, event.orderId());
            });
    }

    private void voidUnpaidStandaloneInvoice(Invoice invoice) {
        if (invoice.getStatus() == InvoiceStatus.VOID) return;
        if (invoice.getPaidAmount() > 0) {
            logger.warn("Standalone invoice dibayar tak di-void otomatis saat cancel: invoiceId={} paid={}",
                invoice.getId(), invoice.getPaidAmount());
            return;
        }
        invoice.voidInvoice();
        invoice.setUpdatedAt(LocalDateTime.now());
        invoiceRepository.save(invoice);
    }

    private void removeCancelledDiningItems(Invoice invoice, Long orderId) {
        boolean hasItems = invoice.getItems().stream()
            .anyMatch(item -> orderId.equals(item.getOrderId()));
        if (!hasItems) return;

        if (invoice.getPaidAmount() > 0) {
            logger.warn("Item order-batal tak dihapus otomatis (invoice sudah ada uang masuk): invoiceId={} orderId={} paid={} — rekonsiliasi manual",
                invoice.getId(), orderId, invoice.getPaidAmount());
            return;
        }

        invoice.getItems().removeIf(item -> orderId.equals(item.getOrderId()));
        invoice.setTotalAmount(invoice.getItems().stream().mapToInt(InvoiceItem::getAmount).sum());
        invoice.setRemainingAmount(invoice.getTotalAmount());
        invoice.setUpdatedAt(LocalDateTime.now());

        if (invoice.getItems().isEmpty()) {
            invoice.voidInvoice();
            logger.info("Invoice dining kosong setelah cancel, di-void: invoiceId={} diningId={}",
                invoice.getId(), invoice.getDiningId());
        } else {
            logger.info("Item order-batal dihapus dari invoice dining: invoiceId={} orderId={} total={}",
                invoice.getId(), orderId, invoice.getTotalAmount());
        }
        invoiceRepository.save(invoice);
    }

    @Transactional
    public InvoiceResponse applyPayment(Long id, ApplyPaymentRequest request) {
        Invoice invoice = findActiveInvoice(id);
        invoice.applyPayment(request.amount());
        Invoice saved = invoiceRepository.save(invoice);
        invoiceEventPublisherService.publishPaid(saved);
        return InvoiceMapper.toResponse(saved);
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
