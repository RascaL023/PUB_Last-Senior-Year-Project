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
import id.my.rascal.payment.api.PaymentApi;
import id.my.rascal.payment.api.event.PaymentSettledEvent;

@Component
public class InvoicePaymentEventListener {

    private static final Logger logger = LoggerFactory.getLogger(InvoicePaymentEventListener.class);

    private final InvoiceService invoiceService;
    private final InvoiceQueryService invoiceQueryService;
    private final PaymentApi paymentApi;

    public InvoicePaymentEventListener(
        InvoiceService invoiceService,
        InvoiceQueryService invoiceQueryService,
        PaymentApi paymentApi
    ) {
        this.invoiceService = invoiceService;
        this.invoiceQueryService = invoiceQueryService;
        this.paymentApi = paymentApi;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentSettled(PaymentSettledEvent event) {
        if (event.settledAmount() == null || event.settledAmount() <= 0) {
            logger.warn("Skipping settlement without amount: paymentId={} invoiceId={}",
                event.paymentId(), event.invoiceId());
            return;
        }

        InvoiceResponse invoice = invoiceQueryService.findActiveInvoiceById(event.invoiceId());

        if (invoice.status() == InvoiceStatus.PAID && invoice.paidAmount() >= event.settledAmount()) {
            logger.error("Skipping replayed payment settlement: paymentId={} invoiceId={}",
                event.paymentId(), event.invoiceId());
            return;
        }

        int applied;
        int excess;
        if (invoice.status() == InvoiceStatus.VOID) {
            applied = 0;
            excess = event.settledAmount();
            logger.warn("Parking settlement on VOID invoice: paymentId={} invoiceId={} excess={}",
                event.paymentId(), event.invoiceId(), excess);
        } else {
            applied = Math.min(invoice.remainingAmount(), event.settledAmount());
            excess = event.settledAmount() - applied;
            if (applied > 0)
                invoiceService.applyPayment(event.invoiceId(), applied);
            if (excess > 0)
                logger.warn("Parking excess settlement: paymentId={} invoiceId={} applied={} excess={}",
                    event.paymentId(), event.invoiceId(), applied, excess);
        }

        paymentApi.confirmSplit(event.paymentId(), applied, excess);
    }

}
