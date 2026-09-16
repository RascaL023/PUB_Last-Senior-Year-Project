package id.my.rascal.invoice.api.event;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Dipublikasikan hanya saat invoice benar-benar lunas (bukan pembayaran parsial).
 *
 * <p>B11: {@code items} saat ini belum punya konsumen (report membaca langsung domain).
 * Dipertahankan sebagai bagian kontrak untuk konsumen async di masa depan (notifikasi,
 * queue, dsb.) — kalau suatu saat diputuskan tidak ada konsumennya, hapus bersama
 * {@link ItemLine} supaya kontrak tidak memuat janji kosong.
 */
public record InvoicePaidEvent(
    Long invoiceId,
    String invoiceNumber,
    Long diningId,
    Integer totalAmount,
    Integer paidAmount,
    Integer remainingAmount,
    List<ItemLine> items,
    LocalDateTime paidAt
) {

    /** Snapshot baris tagihan saat invoice lunas (tanpa pembayaran parsial). */
    public record ItemLine(
        Long menuId,
        String itemName,
        int quantity,
        long amount
    ) {}

}
