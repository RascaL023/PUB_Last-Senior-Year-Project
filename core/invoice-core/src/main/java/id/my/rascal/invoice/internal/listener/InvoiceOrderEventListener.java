package id.my.rascal.invoice.internal.listener;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import id.my.rascal.invoice.internal.service.InvoiceService;
import id.my.rascal.order.api.event.OrderCancelledEvent;
import id.my.rascal.order.api.event.StandaloneOrderCreatedEvent;

@Component
public class InvoiceOrderEventListener {

    private final InvoiceService invoiceService;

    public InvoiceOrderEventListener(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional
    public void onStandaloneOrderCreated(StandaloneOrderCreatedEvent event) {
        invoiceService.handleStandaloneOrderCreated(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional
    public void onOrderCancelled(OrderCancelledEvent event) {
        invoiceService.handleOrderCancelled(event);
    }

}
