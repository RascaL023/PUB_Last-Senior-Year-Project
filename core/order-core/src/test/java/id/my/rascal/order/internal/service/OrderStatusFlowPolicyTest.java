package id.my.rascal.order.internal.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.order.internal.entity.Order;
import id.my.rascal.order.internal.entity.OrderItem;
import id.my.rascal.order.internal.model.enums.OrderPaidStatus;
import id.my.rascal.order.internal.model.enums.OrderStatus;
import id.my.rascal.order.internal.model.enums.OrderType;

import java.util.ArrayList;

class OrderStatusFlowPolicyTest {

    private OrderStatusFlowPolicy policy;

    @BeforeEach
    void setUp() {
        policy = new OrderStatusFlowPolicy();
    }

    @Test
    void dineIn_createdToConfirmed_noPaymentRequired() {
        Order order = createOrder(OrderType.DINE_IN, OrderStatus.CREATED, OrderPaidStatus.UNAPAID);

        assertDoesNotThrow(() -> policy.validateTransition(order, OrderStatus.CONFIRMED));
    }

    @Test
    void takeaway_createdToConfirmed_requiresPaid() {
        Order order = createOrder(OrderType.TAKEAWAY, OrderStatus.CREATED, OrderPaidStatus.UNAPAID);

        assertThrows(BadRequestException.class, () -> policy.validateTransition(order, OrderStatus.CONFIRMED));
    }

    @Test
    void takeaway_createdToConfirmed_withPaid_success() {
        Order order = createOrder(OrderType.TAKEAWAY, OrderStatus.CREATED, OrderPaidStatus.PAID);

        assertDoesNotThrow(() -> policy.validateTransition(order, OrderStatus.CONFIRMED));
    }

    @Test
    void anyType_confirmedToCancelled_success() {
        Order orderDineIn = createOrder(OrderType.DINE_IN, OrderStatus.CONFIRMED, OrderPaidStatus.UNAPAID);
        Order orderTakeaway = createOrder(OrderType.TAKEAWAY, OrderStatus.CONFIRMED, OrderPaidStatus.PAID);

        assertDoesNotThrow(() -> policy.validateTransition(orderDineIn, OrderStatus.CANCELLED));
        assertDoesNotThrow(() -> policy.validateTransition(orderTakeaway, OrderStatus.CANCELLED));
    }

    @Test
    void anyType_createdToCancelled_success() {
        Order order = createOrder(OrderType.DINE_IN, OrderStatus.CREATED, OrderPaidStatus.UNAPAID);

        assertDoesNotThrow(() -> policy.validateTransition(order, OrderStatus.CANCELLED));
    }

    @Test
    void readyToCompleted_requiresPaid() {
        Order orderUnpaid = createOrder(OrderType.DINE_IN, OrderStatus.READY, OrderPaidStatus.UNAPAID);
        Order orderPaid = createOrder(OrderType.DINE_IN, OrderStatus.READY, OrderPaidStatus.PAID);

        assertThrows(BadRequestException.class, () -> policy.validateTransition(orderUnpaid, OrderStatus.COMPLETED));
        assertDoesNotThrow(() -> policy.validateTransition(orderPaid, OrderStatus.COMPLETED));
    }

    @Test
    void completed_cannotTransition() {
        Order order = createOrder(OrderType.DINE_IN, OrderStatus.COMPLETED, OrderPaidStatus.PAID);

        assertThrows(BadRequestException.class, () -> policy.validateTransition(order, OrderStatus.PREPARING));
    }

    @Test
    void cancelled_cannotTransition() {
        Order order = createOrder(OrderType.DINE_IN, OrderStatus.CANCELLED, OrderPaidStatus.UNAPAID);

        assertThrows(BadRequestException.class, () -> policy.validateTransition(order, OrderStatus.CONFIRMED));
    }

    @Test
    void confirmedToPreparing_success() {
        Order order = createOrder(OrderType.DINE_IN, OrderStatus.CONFIRMED, OrderPaidStatus.UNAPAID);

        assertDoesNotThrow(() -> policy.validateTransition(order, OrderStatus.PREPARING));
    }

    @Test
    void preparingToReady_success() {
        Order order = createOrder(OrderType.DINE_IN, OrderStatus.PREPARING, OrderPaidStatus.UNAPAID);

        assertDoesNotThrow(() -> policy.validateTransition(order, OrderStatus.READY));
    }

    @Test
    void confirmedToReady_invalid() {
        Order order = createOrder(OrderType.DINE_IN, OrderStatus.CONFIRMED, OrderPaidStatus.UNAPAID);

        assertThrows(BadRequestException.class, () -> policy.validateTransition(order, OrderStatus.READY));
    }

    private Order createOrder(OrderType type, OrderStatus status, OrderPaidStatus paidStatus) {
        Order order = new Order();
        order.setType(type);
        order.setStatus(status);
        order.setPaidStatus(paidStatus);
        order.setOrderItems(new ArrayList<>());
        return order;
    }
}
