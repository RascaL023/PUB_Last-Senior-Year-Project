package id.my.rascal.order.internal.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import id.my.rascal.common.exception.NotFoundException;
import id.my.rascal.customer.api.CustomerApi;
import id.my.rascal.invoice.api.InvoiceApi;
import id.my.rascal.order.internal.model.enums.OrderType;
import id.my.rascal.order.internal.model.request.OrderItemRequest;
import id.my.rascal.order.internal.model.request.OrderRequest;
import id.my.rascal.order.internal.repository.OrderRepository;

class OrderServiceCustomerValidationTest {

    private static final Long CUSTOMER_ID = 7L;

    private OrderRepository orderRepository;
    private CustomerApi customerApi;
    private OrderItemService orderItemService;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        customerApi = mock(CustomerApi.class);
        orderItemService = mock(OrderItemService.class);
        orderService = new OrderService(
            orderRepository,
            orderItemService,
            mock(OrderStatusFlowPolicy.class),
            mock(OrderEventPublisherService.class),
            mock(InvoiceApi.class),
            customerApi,
            new TrackTokenGenerator()
        );
        when(orderRepository.existsByOrderNumber(any())).thenReturn(false);
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(orderItemService.buildItems(any(), any())).thenReturn(List.of());
        when(orderItemService.computeTotalPrice(any())).thenReturn(0);
    }

    @Test
    void create_unknownCustomerId_rejectedBeforeSave() {
        when(customerApi.existsById(CUSTOMER_ID)).thenReturn(false);

        NotFoundException thrown = assertThrows(NotFoundException.class,
            () -> orderService.create(takeaway(CUSTOMER_ID)));

        assertTrue(thrown.getMessage().contains("7"));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void create_knownCustomerId_passes() {
        when(customerApi.existsById(CUSTOMER_ID)).thenReturn(true);

        assertDoesNotThrow(() -> orderService.create(takeaway(CUSTOMER_ID)));
        verify(orderRepository).save(any());
    }

    @Test
    void create_nullCustomerId_skipsValidation() {
        assertDoesNotThrow(() -> orderService.create(takeaway(null)));

        verify(customerApi, never()).existsById(any());
        verify(orderRepository).save(any());
    }

    private OrderRequest takeaway(Long customerId) {
        return new OrderRequest(
            customerId, null, null, OrderType.TAKEAWAY,
            List.of(new OrderItemRequest(null, 1L, 1, List.of()))
        );
    }

}
