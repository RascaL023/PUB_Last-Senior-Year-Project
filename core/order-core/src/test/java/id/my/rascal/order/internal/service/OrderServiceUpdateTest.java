package id.my.rascal.order.internal.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.invoice.api.InvoiceApi;
import id.my.rascal.order.internal.entity.Order;
import id.my.rascal.order.internal.model.enums.OrderStatus;
import id.my.rascal.order.internal.model.enums.OrderType;
import id.my.rascal.order.internal.model.request.OrderItemRequest;
import id.my.rascal.order.internal.model.request.OrderPutRequest;
import id.my.rascal.order.internal.repository.OrderRepository;

class OrderServiceUpdateTest {

    private OrderRepository orderRepository;
    private OrderItemService orderItemService;
    private InvoiceApi invoiceApi;
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        orderItemService = mock(OrderItemService.class);
        invoiceApi = mock(InvoiceApi.class);
        orderService = new OrderService(
            orderRepository,
            orderItemService,
            mock(OrderStatusFlowPolicy.class),
            mock(OrderEventPublisherService.class),
            invoiceApi
        );
    }

    @Test
    void putOrder_changeDineInTypeToTakeaway_rejected() {
        Order order = dineInOrder(10L);
        when(orderRepository.findActiveById(10L)).thenReturn(Optional.of(order));

        OrderPutRequest request = new OrderPutRequest(
            null, null, null, OrderType.TAKEAWAY,
            List.of(new OrderItemRequest(1L, 2L, 1, List.of()))
        );

        assertThrows(BadRequestException.class, () -> orderService.update(10L, request));
        verify(orderItemService, never()).replaceItems(any(), any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void putOrder_items_withAppliedPayment_rejected() {
        Order order = takeawayOrder(10L);
        when(orderRepository.findActiveById(10L)).thenReturn(Optional.of(order));
        when(invoiceApi.hasAppliedPayment(10L)).thenReturn(true);

        OrderPutRequest request = new OrderPutRequest(
            null, null, null, OrderType.TAKEAWAY,
            List.of(new OrderItemRequest(1L, 2L, 2, List.of()))
        );

        assertThrows(BadRequestException.class, () -> orderService.update(10L, request));
        verify(orderItemService, never()).replaceItems(any(), any());
        verify(orderRepository, never()).save(any());
    }

    private Order dineInOrder(Long id) {
        Order order = new Order();
        order.setId(id);
        order.setOrderNumber("ORD-DINE");
        order.setType(OrderType.DINE_IN);
        order.setStatus(OrderStatus.CREATED);
        order.setTotalPrice(0);
        order.setCreatedAt(LocalDateTime.now());
        return order;
    }

    private Order takeawayOrder(Long id) {
        Order order = dineInOrder(id);
        order.setType(OrderType.TAKEAWAY);
        order.setOrderNumber("ORD-TAKE");
        return order;
    }

}
