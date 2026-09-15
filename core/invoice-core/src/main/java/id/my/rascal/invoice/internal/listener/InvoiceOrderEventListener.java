package id.my.rascal.invoice.internal.listener;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import id.my.rascal.invoice.internal.service.InvoiceService;
import id.my.rascal.order.api.event.OrderCancelledEvent;
import id.my.rascal.order.api.event.OrderDeletedEvent;
import id.my.rascal.order.api.event.OrderItemsChangedEvent;
import id.my.rascal.order.api.event.StandaloneOrderCreatedEvent;

@Component
public class InvoiceOrderEventListener {

    private final InvoiceService invoiceService;

    public InvoiceOrderEventListener(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStandaloneOrderCreated(StandaloneOrderCreatedEvent event) {
        invoiceService.handleStandaloneOrderCreated(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCancelled(OrderCancelledEvent event) {
        invoiceService.handleOrderCancelled(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderDeleted(OrderDeletedEvent event) {
        invoiceService.handleOrderDeleted(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderItemsChanged(OrderItemsChangedEvent event) {
        invoiceService.handleOrderItemsChanged(event);
    }

}
