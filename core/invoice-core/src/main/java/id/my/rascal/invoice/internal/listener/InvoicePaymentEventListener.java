package id.my.rascal.invoice.internal.listener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import id.my.rascal.invoice.internal.entity.InvoiceStatus;
import id.my.rascal.invoice.internal.model.response.InvoiceResponse;
import id.my.rascal.invoice.internal.service.InvoiceEventPublisherService;
import id.my.rascal.invoice.internal.service.InvoiceQueryService;
import id.my.rascal.invoice.internal.service.InvoiceService;
import id.my.rascal.payment.api.event.PaymentSettledEvent;

@Component
public class InvoicePaymentEventListener {

    private static final Logger logger = LoggerFactory.getLogger(InvoicePaymentEventListener.class);

    private final InvoiceService invoiceService;
    private final InvoiceQueryService invoiceQueryService;
    private final InvoiceEventPublisherService invoiceEventPublisherService;

    public InvoicePaymentEventListener(
        InvoiceService invoiceService,
        InvoiceQueryService invoiceQueryService,
        InvoiceEventPublisherService invoiceEventPublisherService
    ) {
        this.invoiceService = invoiceService;
        this.invoiceQueryService = invoiceQueryService;
        this.invoiceEventPublisherService = invoiceEventPublisherService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentSettled(PaymentSettledEvent event) {
        if (!"INVOICE".equals(event.targetType())) return;
        if (event.settledAmount() == null || event.settledAmount() <= 0) {
            logger.warn("Skipping settlement without amount: paymentId={} invoiceId={}",
                event.paymentId(), event.targetId());
            return;
        }

        InvoiceResponse invoice = invoiceQueryService.findActiveInvoiceById(event.targetId());

        if (invoice.status() == InvoiceStatus.PAID && invoice.paidAmount() >= event.settledAmount()) {
            logger.warn("Skipping replayed payment settlement: paymentId={} invoiceId={}",
                event.paymentId(), event.targetId());
            return;
        }

        int applied;
        int excess;
        if (invoice.status() == InvoiceStatus.VOID) {
            applied = 0;
            excess = event.settledAmount();
            logger.warn("Parking settlement on VOID invoice: paymentId={} invoiceId={} excess={}",
                event.paymentId(), event.targetId(), excess);
        } else {
            applied = Math.min(invoice.remainingAmount(), event.settledAmount());
            excess = event.settledAmount() - applied;
            if (applied > 0)
                invoiceService.applyPayment(event.targetId(), applied);
            if (excess > 0)
                logger.warn("Parking excess settlement: paymentId={} invoiceId={} applied={} excess={}",
                    event.paymentId(), event.targetId(), applied, excess);
        }

        invoiceEventPublisherService.publishPaymentApplied(
            event.paymentId(), event.targetId(), applied, excess
        );
    }

}
