package id.my.rascal.invoice.internal.adapter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.invoice.api.InvoiceReportApi;
import id.my.rascal.invoice.internal.repository.InvoiceReportRepository;
import id.my.rascal.invoice.internal.repository.InvoiceReportRepository.MenuSalesAggregate;

@Component
public class InvoiceReportApiImpl implements InvoiceReportApi {

    private static final String VOID = id.my.rascal.invoice.internal.entity.InvoiceStatus.VOID.name();

    private final InvoiceReportRepository invoiceReportRepository;

    public InvoiceReportApiImpl(InvoiceReportRepository invoiceReportRepository) {
        this.invoiceReportRepository = invoiceReportRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public BillingMetrics billingMetrics(LocalDateTime from, LocalDateTime to) {
        var settlement = invoiceReportRepository.findSettlementFacts(from, to);
        var outstanding = invoiceReportRepository.findOutstandingFacts();

        return new BillingMetrics(
            settlement.settledInvoices(),
            settlement.settledAmount(),
            outstanding.outstandingInvoices(),
            outstanding.outstandingAmount()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuSalesRow> topMenuSales(LocalDateTime from, LocalDateTime to, int limit) {
        // SQL sudah membatasi baris (B16); di sini hanya menggabungkan baris yang
        // deskripsinya berbeda tapi menuId-nya sama, lalu memotong hasil akhir.
        Map<Long, MenuSalesRow> byMenuId = new LinkedHashMap<>();
        List<MenuSalesRow> withoutMenuId = new ArrayList<>();

        for (MenuSalesAggregate row : invoiceReportRepository.findMenuSalesBetween(from, to, limit)) {
            if (row.menuId() == null) {
                // Baris invoice manual: tidak ada identitas menu, tetap ditampilkan per deskripsi.
                withoutMenuId.add(new MenuSalesRow(null, row.description(), row.qty(), row.revenue()));
                continue;
            }

            MenuSalesRow existing = byMenuId.get(row.menuId());
            byMenuId.put(row.menuId(), existing == null
                ? new MenuSalesRow(row.menuId(), row.description(), row.qty(), row.revenue())
                : new MenuSalesRow(row.menuId(), existing.description(), existing.qty() + row.qty(), existing.revenue() + row.revenue()));
        }

        return java.util.stream.Stream.concat(byMenuId.values().stream(), withoutMenuId.stream())
            .sorted(Comparator.comparingLong(MenuSalesRow::revenue).reversed())
            .limit(Math.max(limit, 0))
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, String> billingStatusByOrderIds(Collection<Long> orderIds) {
        Map<Long, String> statusByOrderId = new java.util.HashMap<>();
        for (var row : invoiceReportRepository.findStatusByOrderIds(orderIds)) {
            String status = row.status().name();

            // Satu order ideally hanya muncul di satu invoice aktif. Bila ada lebih dari satu,
            // status VOID tidak boleh menutupi invoice yang masih berjalan.
            String current = statusByOrderId.get(row.orderId());
            if (current == null || VOID.equals(current))
                statusByOrderId.put(row.orderId(), status);
        }
        return statusByOrderId;
    }

}
