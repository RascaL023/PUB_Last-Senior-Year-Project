package id.my.rascal.payment.api;

import java.time.LocalDateTime;

/**
 * Read-only contract payment untuk report/dashboard (basis kas).
 *
 * <p>Yang dihitung adalah <b>uang yang benar-benar nempel ke tagihan</b>
 * ({@code applied_amount}), bukan nominal yang diminta ({@code amount}) — kelebihan bayar
 * yang diparkir sebagai {@code excess_amount} bukan penjualan.
 */
public interface PaymentReportApi {

    /**
     * Σ {@code applied_amount} untuk payment yang settlement-nya jatuh di periode
     * ({@code paid_at} dalam {@code [from, to)}).
     *
     * <p>Tidak difilter status sekarang: payment yang belakangan di-refund tetap
     * terhitung sebagai uang masuk pada periode aslinya — refund dicatat terpisah
     * di {@link #sumRefundedBetween}. Dengan begitu angka periode lampau tidak berubah sendiri.
     */
    long sumAppliedSettledBetween(LocalDateTime from, LocalDateTime to);

    /**
     * Σ {@code amount} untuk payment yang di-refund pada periode
     * ({@code refunded_at} dalam {@code [from, to)}) — uang yang keluar.
     */
    long sumRefundedBetween(LocalDateTime from, LocalDateTime to);

}
