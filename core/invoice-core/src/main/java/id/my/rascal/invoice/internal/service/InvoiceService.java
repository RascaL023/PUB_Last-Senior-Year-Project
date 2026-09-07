package id.my.rascal.invoice.internal.service;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.common.exception.NotFoundException;
import id.my.rascal.common.util.StringUtil;
import id.my.rascal.invoice.internal.entity.Invoice;
import id.my.rascal.invoice.internal.entity.InvoiceItem;
import id.my.rascal.invoice.internal.model.mapper.InvoiceMapper;
import id.my.rascal.invoice.internal.model.request.ApplyPaymentRequest;
import id.my.rascal.invoice.internal.model.request.CreateInvoiceRequest;
import id.my.rascal.invoice.internal.model.request.InvoiceItemRequest;
import id.my.rascal.invoice.internal.model.response.InvoiceResponse;
import id.my.rascal.invoice.internal.repository.InvoiceRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

@Service
public class InvoiceService {

    private static final String INVOICE_PREFIX = "INV-";
    private static final String RANDOM_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final Random RANDOM = new Random();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("ddMMyyyy");

    private final InvoiceRepository invoiceRepository;

    public InvoiceService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @Transactional
    public InvoiceResponse create(CreateInvoiceRequest request) {
        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(generateInvoiceNumber());

        List<InvoiceItem> items = request.items().stream()
            .map(itemRequest -> buildItem(invoice, itemRequest))
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
    public InvoiceResponse applyPayment(Long id, ApplyPaymentRequest request) {
        Invoice invoice = findActiveInvoice(id);
        invoice.applyPayment(request.amount());
        return InvoiceMapper.toResponse(invoiceRepository.save(invoice));
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

    private InvoiceItem buildItem(Invoice invoice, InvoiceItemRequest request) {
        if (request.quantity() == null || request.quantity() < 1)
            throw new BadRequestException("Quantity must be at least 1");
        if (request.unitPrice() == null || request.unitPrice() < 0)
            throw new BadRequestException("Unit price cannot be negative");

        InvoiceItem item = new InvoiceItem();
        item.setInvoice(invoice);
        item.setOrderId(request.orderId());
        item.setDescription(StringUtil.normalizeSpaces(request.description()));
        item.setQuantity(request.quantity());
        item.setUnitPrice(request.unitPrice());
        item.setAmount(request.quantity() * request.unitPrice());
        item.setCreatedAt(LocalDateTime.now());
        return item;
    }

    private String generateInvoiceNumber() {
        for (int i = 0; i < 10; i++) {
            String candidate = INVOICE_PREFIX + LocalDateTime.now().format(DATE_FORMAT) + "-" + randomSuffix(6);
            if (!invoiceRepository.existsByInvoiceNumber(candidate))
                return candidate;
        }
        throw new IllegalStateException("Failed to generate unique invoice number");
    }

    private static String randomSuffix(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++)
            sb.append(RANDOM_CHARS.charAt(RANDOM.nextInt(RANDOM_CHARS.length())));
        return sb.toString();
    }

}
