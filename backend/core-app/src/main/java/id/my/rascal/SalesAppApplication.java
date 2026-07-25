package com.rascal.sales;

import com.rascal.sales.order.internal.OrderModuleConfig;
import com.rascal.sales.product.internal.ProductModuleConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

/**
 * COMPOSITION ROOT.
 *
 * Pola wiring modular monolith:
 *  - scanBasePackages hanya ke package milik app sendiri (com.rascal.sales.web)
 *    agar controller ter-deteksi.
 *  - bean dari modul lain TIDAK di-scan langsung, melainkan di-impor secara
 *    eksplisit lewat @Configuration masing-masing modul (ProductModuleConfig,
 *    OrderModuleConfig). Ini membuat batas antar modul jelas & andal di build
 *    reactor Maven (tidak mengandalkan component-scan menyeberangi jar).
 */
@SpringBootApplication(scanBasePackages = "com.rascal.sales.web")
@Import({ProductModuleConfig.class, OrderModuleConfig.class})
public class SalesAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(SalesAppApplication.class, args);
    }
}
