package id.my.rascal.invoice.internal.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import id.my.rascal.invoice.api.event.InvoiceCreatedEvent;
import id.my.rascal.invoice.api.event.InvoicePaidEvent;
import id.my.rascal.invoice.api.event.InvoicePaymentAppliedEvent;
import id.my.rascal.invoice.internal.entity.Invoice;

@Service
public class InvoiceEventPublisherService {

    private final ApplicationEventPublisher eventPublisher;
    private static final Logger logger = LoggerFactory.getLogger(InvoiceEventPublisherService.class);

    public InvoiceEventPublisherService(
        ApplicationEventPublisher eventPublisher
    ) {
        this.eventPublisher = eventPublisher;
    }

    public void publishCreated(Invoice invoice) {
        eventPublisher.publishEvent(new InvoiceCreatedEvent(
            invoice.getId(),
            invoice.getInvoiceNumber(),
            invoice.getDiningId(),
            invoice.getTotalAmount(),
            invoice.getIssuedAt()
        ));
        logger.info("Published invoice event: created invoiceId={}", invoice.getId());
    }

    public void publishPaid(Invoice invoice) {
        eventPublisher.publishEvent(new InvoicePaidEvent(
            invoice.getId(),
            invoice.getInvoiceNumber(),
            invoice.getDiningId(),
            invoice.getTotalAmount(),
            invoice.getPaidAmount(),
            invoice.getRemainingAmount(),
            invoice.getUpdatedAt()
        ));
        logger.info("Published invoice event: paid invoiceId={}", invoice.getId());
    }

    public void publishPaymentApplied(Long paymentId, Long invoiceId, Integer appliedAmount, Integer excessAmount) {
        eventPublisher.publishEvent(new InvoicePaymentAppliedEvent(
            paymentId, invoiceId, appliedAmount, excessAmount
        ));
        logger.info("Published invoice event: paymentApplied paymentId={} invoiceId={} applied={} excess={}",
            paymentId, invoiceId, appliedAmount, excessAmount);
    }

}
