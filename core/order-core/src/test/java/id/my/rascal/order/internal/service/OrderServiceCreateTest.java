package id.my.rascal.order.internal.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.order.internal.model.enums.OrderType;
import id.my.rascal.order.internal.model.request.OrderRequest;
import id.my.rascal.order.internal.repository.OrderRepository;

class OrderServiceCreateTest {

    @Test
    void create_rejectsDineInWithActionableError() {
        OrderRepository orderRepository = mock(OrderRepository.class);
        OrderService orderService = new OrderService(
            orderRepository,
            mock(OrderItemService.class),
            mock(OrderStatusFlowPolicy.class),
            mock(OrderEventPublisherService.class),
            mock(id.my.rascal.invoice.api.InvoiceApi.class)
        );

        OrderRequest request = new OrderRequest(null, null, null, OrderType.DINE_IN, List.of());

        assertThrows(BadRequestException.class, () -> orderService.create(request));
        verify(orderRepository, never()).save(any());
    }

}
