package id.my.rascal.payment.internal.seeder;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.common.seed.Seeder;
import id.my.rascal.common.seed.SeedType;
import id.my.rascal.payment.internal.entity.PaymentMethod;
import id.my.rascal.payment.internal.repository.PaymentMethodRepository;

@Component
@Order(60)
public class FormalPaymentMethodSeeder implements Seeder {

    private static final List<MethodSeed> METHODS = List.of(
        new MethodSeed("CASH", "Tunai"),
        new MethodSeed("BANK_TRANSFER", "Transfer Bank"),
        new MethodSeed("E_WALLET", "E-Wallet"),
        new MethodSeed("QRIS", "QRIS"),
        new MethodSeed("CREDIT_CARD", "Kartu Kredit"),
        new MethodSeed("XENDIT", "Xendit")
    );

    private final PaymentMethodRepository paymentMethodRepository;

    public FormalPaymentMethodSeeder(PaymentMethodRepository paymentMethodRepository) {
        this.paymentMethodRepository = paymentMethodRepository;
    }

    @Override
    public SeedType seedType() {
        return SeedType.FORMAL;
    }

    @Override
    @Transactional
    public void seed() {
        LocalDateTime now = LocalDateTime.now();
        for (MethodSeed seed : METHODS) {
            if (paymentMethodRepository.existsByCode(seed.code())) continue;
            PaymentMethod method = new PaymentMethod();
            method.setCode(seed.code());
            method.setName(seed.name());
            method.setIsActive(true);
            method.setCreatedAt(now);
            paymentMethodRepository.save(method);
        }
    }

    private record MethodSeed(String code, String name) {}
}
