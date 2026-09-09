package id.my.rascal.invoice.internal.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import id.my.rascal.invoice.internal.entity.InvoiceStatus;
import id.my.rascal.invoice.internal.model.response.InvoiceResponse;
import id.my.rascal.invoice.internal.service.InvoiceQueryService;
import id.my.rascal.invoice.internal.service.InvoiceService;
import id.my.rascal.payment.api.event.PaymentSettledEvent;

@Component
public class InvoicePaymentEventListener {

    private static final Logger logger = LoggerFactory.getLogger(InvoicePaymentEventListener.class);

    private final InvoiceService invoiceService;
    private final InvoiceQueryService invoiceQueryService;

    public InvoicePaymentEventListener(
        InvoiceService invoiceService,
        InvoiceQueryService invoiceQueryService
    ) {
        this.invoiceService = invoiceService;
        this.invoiceQueryService = invoiceQueryService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentSettled(PaymentSettledEvent event) {
        if (!"INVOICE".equals(event.targetType())) return;

        if (isReplay(event)) {
            logger.warn("Skipping replayed payment settlement: paymentId={} invoiceId={}",
                event.paymentId(), event.targetId());
            return;
        }

        invoiceService.applyPayment(event.targetId(), event.settledAmount());
    }

    private boolean isReplay(PaymentSettledEvent event) {
        InvoiceResponse invoice = invoiceQueryService.findActiveInvoiceById(event.targetId());
        return invoice.status() == InvoiceStatus.PAID
            && invoice.paidAmount() >= event.settledAmount();
    }

}
