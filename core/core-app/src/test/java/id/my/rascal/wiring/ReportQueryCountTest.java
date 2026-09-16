package id.my.rascal.wiring;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import jakarta.persistence.EntityManagerFactory;
import id.my.rascal.menu.api.MenuApi;
import id.my.rascal.menu.api.MenuApiResponse;
import id.my.rascal.order.internal.model.enums.OrderType;
import id.my.rascal.order.internal.model.request.OrderItemRequest;
import id.my.rascal.order.internal.model.request.OrderRequest;
import id.my.rascal.order.internal.service.OrderService;
import id.my.rascal.report.internal.service.ReportService;

/**
 * B10: penjaga biaya query endpoint dashboard.
 *
 * <p>Satu panggilan {@code getDashboardSummary} menjalankan query per sumber:
 * invoice settlement + piutang (2), menu sales (1), payment (1),
 * dining terbuka + meja (3), order in-progress (1), recent activity (1),
 * billing status per order (1), label menu via MenuApi (mock, 0) — total maksimum 10.
 * Tanpa penjaga ini, penambahan metrik baru bisa membengkak jumlah query diam-diam.
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReportQueryCountTest {

    @TestConfiguration
    static class MockConfig {
        @Bean
        @Primary
        MenuApi menuApi() {
            return mock(MenuApi.class);
        }
    }

    private static final int MAX_QUERIES_PER_SUMMARY = 10;

    @Autowired
    private MenuApi menuApi;
    @Autowired
    private EntityManagerFactory entityManagerFactory;
    @Autowired
    private OrderService orderService;
    @Autowired
    private ReportService reportService;

    private Statistics statistics() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        sessionFactory.getStatistics().setStatisticsEnabled(true);
        return sessionFactory.getStatistics();
    }

    @BeforeAll
    void seed() {
        when(menuApi.getMenuSnapshots(any())).thenAnswer(invocation -> {
            Collection<Long> ids = invocation.getArgument(0);
            if (ids != null && ids.contains(10L))
                return List.of(new MenuApiResponse(10L, "Nasi Goreng", 25000, true, List.of()));
            return List.of();
        });
        when(menuApi.getModifierOptionSnapshots(any())).thenReturn(List.of());

        orderService.create(new OrderRequest(
            null, null, null, OrderType.TAKEAWAY,
            List.of(new OrderItemRequest(null, 10L, 2, List.of()))
        ));
    }

    @Test
    void dashboardSummary_staysBounded() {
        // Panaskan dulu supaya lazy-load sekunder tidak mengotori hitungan.
        reportService.getDashboardSummary(null, null);

        Statistics stats = statistics();
        stats.clear();

        reportService.getDashboardSummary(null, null);

        assertTrue(stats.getQueryExecutionCount() <= MAX_QUERIES_PER_SUMMARY,
            "Dashboard summary took " + stats.getQueryExecutionCount() + " queries (max "
                + MAX_QUERIES_PER_SUMMARY + ")");
    }
}
