package id.my.rascal.wiring;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import org.springframework.data.domain.Pageable;

import jakarta.persistence.EntityManagerFactory;
import id.my.rascal.dining.internal.model.request.DiningTableRequest;
import id.my.rascal.dining.internal.model.request.OpenDiningRequest;
import id.my.rascal.dining.internal.service.DiningService;
import id.my.rascal.dining.internal.service.TableService;
import id.my.rascal.invoice.internal.service.InvoiceQueryService;
import id.my.rascal.menu.api.MenuApi;
import id.my.rascal.menu.api.MenuApiResponse;
import id.my.rascal.order.internal.model.enums.OrderType;
import id.my.rascal.order.internal.model.request.OrderItemRequest;
import id.my.rascal.order.internal.model.request.OrderRequest;
import id.my.rascal.order.internal.model.response.OrderResponse;
import id.my.rascal.order.internal.service.OrderQueryService;
import id.my.rascal.order.internal.service.OrderService;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class BillingQueryCountTest {

    @TestConfiguration
    static class MockConfig {
        @Bean
        @Primary
        MenuApi menuApi() {
            return mock(MenuApi.class);
        }
    }

    @Autowired
    private MenuApi menuApi;
    @Autowired
    private EntityManagerFactory entityManagerFactory;
    @Autowired
    private OrderService orderService;
    @Autowired
    private OrderQueryService orderQueryService;
    @Autowired
    private DiningService diningService;
    @Autowired
    private TableService tableService;
    @Autowired
    private InvoiceQueryService invoiceQueryService;

    private Statistics statistics() {
        SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
        sessionFactory.getStatistics().setStatisticsEnabled(true);
        return sessionFactory.getStatistics();
    }

    @BeforeAll
    void seed() {
        when(menuApi.getMenuSnapshots(List.of(10L))).thenReturn(List.of(
            new MenuApiResponse(10L, "Nasi Goreng", 25000, true, List.of())
        ));
        when(menuApi.getModifierOptionSnapshots(any())).thenReturn(List.of());

        orderService.create(new OrderRequest(
            null, null, null, OrderType.TAKEAWAY,
            List.of(new OrderItemRequest(null, 10L, 2, List.of()))
        ));
        Long tableId = tableService.create(new DiningTableRequest("Q1")).id();
        diningService.open(new OpenDiningRequest(tableId));
    }

    @Test
    void invoiceList_staysBounded() {
        Statistics stats = statistics();
        stats.clear();

        invoiceQueryService.searchActive(null, null, null, null, Pageable.ofSize(10));

        assertTrue(stats.getQueryExecutionCount() <= 4,
            "Invoice list took " + stats.getQueryExecutionCount() + " queries");
    }

    @Test
    void orderDetail_staysBounded() {
        OrderResponse order = orderQueryService.searchActive(null, null, Pageable.ofSize(1))
            .getContent().get(0);

        Statistics stats = statistics();
        stats.clear();

        orderQueryService.findActiveOrderById(order.id());

        assertTrue(stats.getQueryExecutionCount() <= 4,
            "Order detail took " + stats.getQueryExecutionCount() + " queries");
    }

    @Test
    void diningSearch_staysBounded() {
        Statistics stats = statistics();
        stats.clear();

        diningService.search(null, Pageable.ofSize(10));

        assertTrue(stats.getQueryExecutionCount() <= 6,
            "Dining search took " + stats.getQueryExecutionCount() + " queries");
    }

}
