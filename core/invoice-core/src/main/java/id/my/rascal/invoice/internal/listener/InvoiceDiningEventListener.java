package id.my.rascal.invoice.internal.listener;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import id.my.rascal.dining.api.event.DiningOrderAddedEvent;
import id.my.rascal.invoice.internal.service.InvoiceService;

@Component
public class InvoiceDiningEventListener {

    private final InvoiceService invoiceService;

    public InvoiceDiningEventListener(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional
    public void onDiningOrderAdded(DiningOrderAddedEvent event) {
        invoiceService.handleOrderAddedToDining(event);
    }

}
