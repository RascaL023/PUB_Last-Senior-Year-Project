package id.my.rascal.order.internal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import id.my.rascal.common.exception.NotFoundException;
import id.my.rascal.customer.api.CustomerApi;
import id.my.rascal.invoice.api.InvoiceApi;
import id.my.rascal.order.api.OrderItemDetail;
import id.my.rascal.order.internal.entity.Order;
import id.my.rascal.order.internal.entity.OrderItem;
import id.my.rascal.order.internal.entity.OrderItemModifier;
import id.my.rascal.order.internal.model.enums.OrderStatus;
import id.my.rascal.order.internal.model.enums.OrderType;
import id.my.rascal.order.internal.model.response.GuestOrderTrackingResponse;
import id.my.rascal.order.internal.repository.OrderItemRepository;
import id.my.rascal.order.internal.repository.OrderRepository;

class GuestOrderTrackingTest {

    private static final String TOKEN = "tracking-token";

    private OrderRepository orderRepository;
    private InvoiceApi invoiceApi;
    private OrderQueryService orderQueryService;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        invoiceApi = mock(InvoiceApi.class);
        orderQueryService = new OrderQueryService(
            orderRepository,
            mock(OrderItemRepository.class),
            mock(CustomerApi.class),
            invoiceApi
        );
    }

    @Test
    void trackByToken_returnsOrderWithItemsAndInvoiceStatus() {
        when(orderRepository.findActiveByTrackToken(TOKEN)).thenReturn(Optional.of(order()));
        when(invoiceApi.findStatusByOrderId(ORDER_ID)).thenReturn("OPEN");

        GuestOrderTrackingResponse response = orderQueryService.findActiveByTrackToken(TOKEN);

        assertEquals("ORD-01092026-ABC123", response.orderNumber());
        assertEquals("TAKEAWAY", response.type());
        assertEquals(OrderStatus.PREPARING.name(), response.status());
        assertEquals(45_000, response.totalPrice());
        assertEquals("OPEN", response.invoiceStatus());
        assertEquals(1, response.items().size());
        assertEquals("Es Kopi Susu", response.items().get(0).itemName());
        assertEquals("Less sugar", response.items().get(0).modifiers().get(0).modifierName());
    }

    @Test
    void trackByToken_withoutInvoice_invoiceStatusIsNull() {
        when(orderRepository.findActiveByTrackToken(TOKEN)).thenReturn(Optional.of(order()));
        when(invoiceApi.findStatusByOrderId(ORDER_ID)).thenReturn(null);

        GuestOrderTrackingResponse response = orderQueryService.findActiveByTrackToken(TOKEN);

        assertNull(response.invoiceStatus());
    }

    @Test
    void trackByToken_unknownToken_isNotFoundWithGenericMessage() {
        when(orderRepository.findActiveByTrackToken("wrong-token")).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
            NotFoundException.class,
            () -> orderQueryService.findActiveByTrackToken("wrong-token")
        );

        assertEquals("Order not found", exception.getMessage());
    }

    @Test
    void trackByToken_deletedOrder_isNotFound() {
        when(orderRepository.findActiveByTrackToken(TOKEN)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> orderQueryService.findActiveByTrackToken(TOKEN));
    }

    private static final Long ORDER_ID = 21L;

    private Order order() {
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setOrderNumber("ORD-01092026-ABC123");
        order.setTrackToken(TOKEN);
        order.setStatus(OrderStatus.PREPARING);
        order.setType(OrderType.TAKEAWAY);
        order.setTotalPrice(45_000);

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setMenuId(1L);
        item.setItemName("Es Kopi Susu");
        item.setUnitPrice(22_000);
        item.setQuantity(2);
        item.setSubtotal(45_000);

        OrderItemModifier modifier = new OrderItemModifier();
        modifier.setOrderItem(item);
        modifier.setModifierOptionId(13L);
        modifier.setName("Less sugar");
        modifier.setAdditionalPrice(0);
        item.setModifiers(List.of(modifier));

        order.setOrderItems(List.of(item));
        return order;
    }

}
