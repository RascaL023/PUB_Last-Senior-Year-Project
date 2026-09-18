package id.my.rascal.order.internal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import id.my.rascal.dining.api.DiningReportApi;
import id.my.rascal.order.internal.entity.Order;
import id.my.rascal.order.internal.entity.OrderItem;
import id.my.rascal.order.internal.model.enums.OrderStatus;
import id.my.rascal.order.internal.model.enums.OrderType;
import id.my.rascal.order.internal.model.response.KitchenTicketResponse;
import id.my.rascal.order.internal.repository.OrderItemRepository;
import id.my.rascal.order.internal.repository.OrderRepository;

class KitchenServiceTest {

    private OrderRepository orderRepository;
    private OrderItemRepository orderItemRepository;
    private DiningReportApi diningReportApi;
    private KitchenService kitchenService;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        orderItemRepository = mock(OrderItemRepository.class);
        diningReportApi = mock(DiningReportApi.class);
        kitchenService = new KitchenService(orderRepository, orderItemRepository, diningReportApi);

        when(orderItemRepository.findActiveByOrderIds(any())).thenReturn(List.of());
        when(diningReportApi.tableNumbersByOrderIds(any())).thenReturn(Map.of());
    }

    @Test
    void getQueue_defaultStatuses_areConfirmedAndPreparing_oldestFirst() {
        stubOrders(List.of(order(1L, "ORD-1")));

        kitchenService.getQueue(null, KitchenService.DEFAULT_SIZE);

        assertEquals(List.of(OrderStatus.CONFIRMED, OrderStatus.PREPARING), capturedStatuses());
        Sort.Order createdAt = capturedPageable().getSort().getOrderFor("createdAt");
        assertEquals(Sort.Direction.ASC, createdAt.getDirection());
        assertEquals(KitchenService.DEFAULT_SIZE, capturedPageable().getPageSize());
    }

    @Test
    void getQueue_sizeIsCappedAtMax() {
        stubOrders(List.of(order(1L, "ORD-1")));

        kitchenService.getQueue(null, 999);

        assertEquals(KitchenService.MAX_SIZE, capturedPageable().getPageSize());
    }

    @Test
    void getQueue_mapsTableNumberAndItemsPerTicket() {
        stubOrders(List.of(order(1L, "ORD-1"), order(2L, "ORD-2")));
        when(diningReportApi.tableNumbersByOrderIds(any())).thenReturn(Map.of(1L, "T-04"));
        when(orderItemRepository.findActiveByOrderIds(any())).thenReturn(List.of(
            item(order(1L, "ORD-1"), "Nasi Goreng", 2),
            item(order(2L, "ORD-2"), "Espresso", 1)
        ));

        List<KitchenTicketResponse> queue = kitchenService.getQueue(null, KitchenService.DEFAULT_SIZE);

        assertEquals(2, queue.size());
        assertEquals("T-04", queue.get(0).tableNumber());
        assertEquals(1, queue.get(0).items().size());
        assertEquals("Nasi Goreng", queue.get(0).items().get(0).itemName());
        assertNull(queue.get(1).tableNumber());
        assertEquals("Espresso", queue.get(1).items().get(0).itemName());
    }

    @Test
    void getQueue_emptyResult_skipsContractCalls() {
        stubOrders(List.of());

        assertEquals(List.of(), kitchenService.getQueue(null, KitchenService.DEFAULT_SIZE));

        verify(diningReportApi, org.mockito.Mockito.never()).tableNumbersByOrderIds(any());
    }

    @SuppressWarnings("unchecked")
    private List<OrderStatus> capturedStatuses() {
        ArgumentCaptor<Collection<OrderStatus>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(orderRepository).searchActive(isNull(), captor.capture(), any());
        return List.copyOf(captor.getValue());
    }

    private Pageable capturedPageable() {
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(orderRepository, org.mockito.Mockito.atLeastOnce()).searchActive(isNull(), any(), captor.capture());
        return captor.getValue();
    }

    private void stubOrders(List<Order> orders) {
        when(orderRepository.searchActive(isNull(), any(), any())).thenReturn(new PageImpl<>(orders));
    }

    private Order order(Long id, String orderNumber) {
        Order order = new Order();
        order.setId(id);
        order.setOrderNumber(orderNumber);
        order.setType(OrderType.DINE_IN);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setTotalPrice(0);
        return order;
    }

    private OrderItem item(Order order, String itemName, int quantity) {
        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setItemName(itemName);
        item.setQuantity(quantity);
        item.setUnitPrice(10000);
        item.setSubtotal(10000 * quantity);
        return item;
    }

}
