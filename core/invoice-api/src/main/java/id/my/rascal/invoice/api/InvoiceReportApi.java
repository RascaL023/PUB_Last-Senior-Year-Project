package id.my.rascal.invoice.api;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Read-only contract invoice untuk report/dashboard.
 *
 * <p>Dua kelompok angka yang sengaja dipisah supaya tidak tercampur:
 * <ul>
 *   <li><b>Fakta pelunasan (jurnal)</b> — dihitung dari {@code paidAt}/{@code settledAmount} yang
 *       dibekukan saat invoice lunas; angka periode lampau tidak berubah sendiri.</li>
 *   <li><b>Snapshot keadaan sekarang</b> — {@code outstandingInvoices}/{@code outstandingAmount}
 *       (piutang berjalan), bukan filter periode.</li>
 * </ul>
 */
public interface InvoiceReportApi {

    /**
     * @param from inklusif, {@code to} eksklusif (zona waktu server)
     */
    BillingMetrics billingMetrics(LocalDateTime from, LocalDateTime to);

    /**
     * Penjualan menu dari invoice yang dilunasi pada periode: seluruh baris invoice sehingga
     * totalnya bisa direkonsiliasi dengan {@link BillingMetrics#settledAmount()}.
     */
    List<MenuSalesRow> topMenuSales(LocalDateTime from, LocalDateTime to, int limit);

    /**
     * Status billing per order dari invoice aktif yang memuat order tersebut — untuk memperkaya
     * aktivitas terbaru. Order tanpa invoice aktif tidak ada di map.
     */
    Map<Long, String> billingStatusByOrderIds(Collection<Long> orderIds);

    /**
     * @param settledInvoices jumlah invoice yang dilunasi pada periode (berdasarkan {@code paidAt})
     * @param settledAmount   Σ nilai tagihan saat dilunasi pada periode ({@code settledAmount})
     * @param outstandingInvoices jumlah invoice yang belum lunas sekarang ({@code OPEN}/{@code PARTIALLY_PAID})
     * @param outstandingAmount   Σ sisa tagihan ({@code remainingAmount}) saat ini
     */
    record BillingMetrics(
        long settledInvoices,
        long settledAmount,
        long outstandingInvoices,
        long outstandingAmount
    ) {
        public long averageSettledInvoice() {
            return settledInvoices == 0 ? 0 : settledAmount / settledInvoices;
        }
    }

    /**
     * @param menuId      bisa {@code null} untuk baris invoice manual
     * @param description nama baris saat tagihan dibuat (dipakai sebagai fallback label)
     */
    record MenuSalesRow(
        Long menuId,
        String description,
        long qty,
        long revenue
    ) {}

}
