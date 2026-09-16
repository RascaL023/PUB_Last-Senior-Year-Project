package id.my.rascal.invoice.internal.adapter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.invoice.api.InvoiceReportApi;
import id.my.rascal.invoice.internal.entity.InvoiceStatus;
import id.my.rascal.invoice.internal.repository.InvoiceReportRepository;

@Component
public class InvoiceReportApiImpl implements InvoiceReportApi {

    private static final String VOID = InvoiceStatus.VOID.name();

    private final InvoiceReportRepository invoiceReportRepository;

    public InvoiceReportApiImpl(InvoiceReportRepository invoiceReportRepository) {
        this.invoiceReportRepository = invoiceReportRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public BillingMetrics billingMetrics(LocalDateTime from, LocalDateTime to) {
        Object[] settlement = invoiceReportRepository.findSettlementFacts(from, to);
        Object[] outstanding = invoiceReportRepository.findOutstandingFacts();

        return new BillingMetrics(
            toLong(settlement[0]),
            toLong(settlement[1]),
            toLong(outstanding[0]),
            toLong(outstanding[1])
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuSalesRow> topMenuSales(LocalDateTime from, LocalDateTime to, int limit) {
        Map<Long, MenuSalesRow> byMenuId = new LinkedHashMap<>();
        List<MenuSalesRow> withoutMenuId = new ArrayList<>();

        for (Object[] row : invoiceReportRepository.findMenuSalesBetween(from, to)) {
            Long menuId = (Long) row[0];
            String description = (String) row[1];
            long qty = toLong(row[2]);
            long revenue = toLong(row[3]);

            if (menuId == null) {
                // Baris invoice manual: tidak ada identitas menu, tetap ditampilkan per deskripsi.
                withoutMenuId.add(new MenuSalesRow(null, description, qty, revenue));
                continue;
            }

            // Satu menu bisa muncul dengan beberapa deskripsi (mis. menu berganti nama).
            MenuSalesRow existing = byMenuId.get(menuId);
            byMenuId.put(menuId, existing == null
                ? new MenuSalesRow(menuId, description, qty, revenue)
                : new MenuSalesRow(menuId, existing.description(), existing.qty() + qty, existing.revenue() + revenue));
        }

        return java.util.stream.Stream.concat(byMenuId.values().stream(), withoutMenuId.stream())
            .sorted(Comparator.comparingLong(MenuSalesRow::revenue).reversed())
            .limit(Math.max(limit, 0))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, String> billingStatusByOrderIds(Collection<Long> orderIds) {
        Map<Long, String> statusByOrderId = new HashMap<>();
        for (Object[] row : invoiceReportRepository.findStatusByOrderIds(orderIds)) {
            Long orderId = (Long) row[0];
            String status = ((InvoiceStatus) row[1]).name();

            // Satu order ideally hanya muncul di satu invoice aktif. Bila ada lebih dari satu,
            // status VOID tidak boleh menutupi invoice yang masih berjalan.
            String current = statusByOrderId.get(orderId);
            if (current == null || VOID.equals(current))
                statusByOrderId.put(orderId, status);
        }
        return statusByOrderId;
    }

    private static long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

}
