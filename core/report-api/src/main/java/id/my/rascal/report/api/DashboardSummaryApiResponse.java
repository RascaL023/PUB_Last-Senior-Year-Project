package id.my.rascal.report.api;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Ringkasan dashboard.
 *
 * <p>Angka finansial dipisah menjadi dua basis yang <b>tidak boleh dicampur</b>:
 * <ul>
 *   <li>{@code sales.cash} — uang yang benar-benar masuk/keluar lewat payment
 *       ({@code applied_amount}), jadi hanya jalur {@code POST /payments}.</li>
 *   <li>{@code sales.billing} — nilai tagihan invoice saat lunas ({@code settled_amount}),
 *       termasuk pelunasan yang dicatat langsung di invoice.</li>
 * </ul>
 * Dua angka itu wajar berbeda (mis. pelunasan manual tanpa payment record, atau kelebihan bayar
 * yang diparkir) — makanya tidak pernah digabung menjadi satu "revenue".
 */
public record DashboardSummaryApiResponse(
    Period period,
    Sales sales,
    Operations operations,
    List<TopMenuEntry> topMenus,
    List<RecentActivityEntry> recentActivity
) {

    public record Period(
        LocalDate from,
        LocalDate to
    ) {}

    public record Sales(
        Cash cash,
        Billing billing
    ) {}

    /**
     * Basis kas.
     *
     * @param received Σ {@code applied_amount} payment yang settlement-nya di periode
     * @param refunded Σ nominal payment yang di-refund pada periode
     * @param net      {@code received - refunded} (bisa negatif bila refund periode ini
     *                 berasal dari penjualan periode sebelumnya)
     */
    public record Cash(
        long received,
        long refunded,
        long net
    ) {}

    /**
     * Basis tagihan.
     *
     * @param settledInvoices      jumlah invoice yang dilunasi pada periode (waktu pelunasan)
     * @param settledAmount        Σ nilai tagihan saat dilunasi pada periode (dibekukan saat lunas)
     * @param averageSettledInvoice {@code settledAmount / settledInvoices}, {@code 0} bila kosong
     * @param refundedAmount       Σ koreksi tagihan ({@code Refund.scopeAmount}) pada periode
     * @param outstandingInvoices  snapshot: jumlah invoice belum lunas saat ini
     * @param outstandingAmount    snapshot: Σ sisa tagihan ({@code remaining_amount}) saat ini
     */
    public record Billing(
        long settledInvoices,
        long settledAmount,
        long averageSettledInvoice,
        long refundedAmount,
        long outstandingInvoices,
        long outstandingAmount
    ) {}

    public record Operations(
        long openDinings,
        long occupiedTables,
        long availableTables,
        long ordersInProgress
    ) {}

    /**
     * Penjualan menu bruto dari invoice yang lunas pada periode; totalnya sejalan dengan
     * {@code sales.billing.settledAmount} sehingga bisa direkonsiliasi.
     * {@code menuId} bisa {@code null} untuk baris invoice manual.
     */
    public record TopMenuEntry(
        Long menuId,
        String name,
        long qty,
        long revenue
    ) {}

    /**
     * Aktivitas terbaru tetap berbasis order (unit operasional), tetapi status billing-nya
     * berasal dari invoice aktif: {@code OPEN}/{@code PARTIALLY_PAID}/{@code PAID}/{@code VOID},
     * atau {@code null} bila order tidak punya invoice aktif.
     */
    public record RecentActivityEntry(
        Long orderId,
        String orderNumber,
        String status,
        String billingStatus,
        long totalPrice,
        LocalDateTime createdAt
    ) {}

}
