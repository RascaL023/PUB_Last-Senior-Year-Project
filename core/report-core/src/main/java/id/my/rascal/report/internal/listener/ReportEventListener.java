package id.my.rascal.report.internal.listener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.dao.DataIntegrityViolationException;

import id.my.rascal.order.api.OrderPaidEvent;
import id.my.rascal.payment.api.PaymentRefundedEvent;
import id.my.rascal.payment.api.PaymentSettledEvent;
import id.my.rascal.report.internal.entity.ReportDailySales;
import id.my.rascal.report.internal.entity.ReportMenuDaily;
import id.my.rascal.report.internal.entity.ReportProcessedEvent;
import id.my.rascal.report.internal.repository.ReportDailySalesRepository;
import id.my.rascal.report.internal.repository.ReportMenuDailyRepository;
import id.my.rascal.report.internal.repository.ReportProcessedEventRepository;

@Component
public class ReportEventListener {

    private static final Logger log = LoggerFactory.getLogger(ReportEventListener.class);
    private static final ZoneId JAKARTA = ZoneId.of("Asia/Jakarta");

    private static final String TYPE_ORDER_PAID = "ORDER_PAID";
    private static final String TYPE_PAYMENT_SETTLED = "PAYMENT_SETTLED";
    private static final String TYPE_PAYMENT_REFUNDED = "PAYMENT_REFUNDED";

    private final ReportDailySalesRepository dailySalesRepository;
    private final ReportMenuDailyRepository menuDailyRepository;
    private final ReportProcessedEventRepository processedEventRepository;

    public ReportEventListener(
        ReportDailySalesRepository dailySalesRepository,
        ReportMenuDailyRepository menuDailyRepository,
        ReportProcessedEventRepository processedEventRepository
    ) {
        this.dailySalesRepository = dailySalesRepository;
        this.menuDailyRepository = menuDailyRepository;
        this.processedEventRepository = processedEventRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleOrderPaid(OrderPaidEvent event) {
        String eventKey = "order:" + event.orderId();
        try {
            if (alreadyProcessed(TYPE_ORDER_PAID, eventKey)) return;
            LocalDate saleDate = bucketDate(event.paidAt());

            for (OrderPaidEvent.ItemLine line : event.items()) {
                upsertMenuDaily(saleDate, line);
            }
            markProcessed(TYPE_ORDER_PAID, eventKey);
            log.debug("Projection menu daily updated for order {} ({} lines)", event.orderId(), event.items().size());
        } catch (Exception e) {
            log.error("Failed to project OrderPaidEvent for order {}: {}", event.orderId(), e.getMessage(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentSettled(PaymentSettledEvent event) {
        String eventKey = "payment:" + event.externalId();
        try {
            if (alreadyProcessed(TYPE_PAYMENT_SETTLED, eventKey)) return;
            LocalDate saleDate = bucketDate(event.paidAt());
            upsertDailySales(saleDate, event.amount());
            markProcessed(TYPE_PAYMENT_SETTLED, eventKey);
            log.debug("Projection daily sales +{} for payment {}", event.amount(), event.paymentId());
        } catch (Exception e) {
            log.error("Failed to project PaymentSettledEvent for payment {}: {}", event.paymentId(), e.getMessage(), e);
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handlePaymentRefunded(PaymentRefundedEvent event) {
        String eventKey = "payment:" + event.paymentId() + ":refund";
        try {
            if (alreadyProcessed(TYPE_PAYMENT_REFUNDED, eventKey)) return;
            if (event.originalPaidAt() == null) {
                log.warn("Refund for payment {} has no paidAt; skipped", event.paymentId());
                return;
            }
            LocalDate saleDate = bucketDate(event.originalPaidAt());
            addDailySales(saleDate, -event.amount());
            markProcessed(TYPE_PAYMENT_REFUNDED, eventKey);
            log.debug("Projection daily sales {} (refund payment {})", -event.amount(), event.paymentId());
        } catch (Exception e) {
            log.error("Failed to project PaymentRefundedEvent for payment {}: {}", event.paymentId(), e.getMessage(), e);
        }
    }

    // ---- helpers ----

    private boolean alreadyProcessed(String eventType, String eventKey) {
        if (processedEventRepository.existsByEventTypeAndEventKey(eventType, eventKey)) {
            log.debug("Duplicate {} ignored (key={})", eventType, eventKey);
            return true;
        }
        return false;
    }

    private void markProcessed(String eventType, String eventKey) {
        processedEventRepository.save(new ReportProcessedEvent(eventType, eventKey, LocalDateTime.now()));
    }

    private void upsertDailySales(LocalDate saleDate, long delta) {
        if (dailySalesRepository.findBySaleDate(saleDate).isEmpty()) {
            try {
                dailySalesRepository.save(new ReportDailySales(saleDate, delta));
                return;
            } catch (DataIntegrityViolationException e) {
                // Baris dibuat thread lain di antara find dan save — lanjut sebagai increment.
            }
        }
        addDailySales(saleDate, delta);
    }

    private void addDailySales(LocalDate saleDate, long delta) {
        dailySalesRepository.addRevenue(saleDate, delta);
    }

    private void upsertMenuDaily(LocalDate saleDate, OrderPaidEvent.ItemLine line) {
        String menuName = line.itemName();
        long qty = line.quantity();
        long revenue = line.subtotal();

        if (menuDailyRepository.findBySaleDateAndMenuName(saleDate, menuName).isEmpty()) {
            try {
                menuDailyRepository.save(new ReportMenuDaily(saleDate, line.menuId(), menuName, qty, revenue));
                return;
            } catch (DataIntegrityViolationException e) {
                // Baris dibuat thread lain di antara find dan save — lanjut sebagai increment.
            }
        }
        menuDailyRepository.addQtyAndRevenue(saleDate, menuName, qty, revenue);
    }

    /** Bucket tanggal penjualan berbasis WIB dari paidAt (disimpan dalam zona server). */
    private LocalDate bucketDate(LocalDateTime paidAt) {
        return paidAt.atZone(ZoneId.systemDefault())
            .withZoneSameInstant(JAKARTA)
            .toLocalDate();
    }

}
