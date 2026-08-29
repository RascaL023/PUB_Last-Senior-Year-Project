package id.my.rascal.order.internal.service;

import org.springframework.stereotype.Service;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.order.internal.entity.Order;
import id.my.rascal.order.internal.model.enums.OrderPaidStatus;
import id.my.rascal.order.internal.model.enums.OrderStatus;
import id.my.rascal.order.internal.model.enums.OrderType;

@Service
public class OrderStatusFlowPolicy {

    public void validateTransition(
        Order order,
        OrderStatus nextStatus
    ) {
        OrderStatus currentStatus = order.getStatus();
        if (isTerminal(currentStatus))
            reject( "Order with status " + currentStatus + " cannot be changed");

        switch (currentStatus) {
            case CREATED -> validateFromCreated(order, nextStatus);
            case CONFIRMED -> validateFromConfirmed(nextStatus);
            case PREPARING -> validateFromPreparing(nextStatus);
            case READY -> validateFromReady(order, nextStatus);
            default -> reject();
        }
    }

    private void validateFromCreated(
        Order order,
        OrderStatus n
    ) {
        if (n == OrderStatus.CANCELLED)
            return;
        else if (n != OrderStatus.CONFIRMED)
            reject();

        if (
            order.getType() == OrderType.TAKEAWAY && 
            order.getPaidStatus() != OrderPaidStatus.PAID
        ) reject("Takeaway order must be paid before confirmation");
    }

    private void validateFromConfirmed(OrderStatus n) {
        if (n != OrderStatus.PREPARING)
            reject();
    }

    private void validateFromPreparing(OrderStatus n) {
        if (n != OrderStatus.READY) {
            reject();
        }
    }

    private void validateFromReady(
        Order order,
        OrderStatus nextStatus
    ) {
        if (nextStatus != OrderStatus.COMPLETED)
            reject();

        if (!order.getPaidStatus().equals(OrderPaidStatus.PAID))
            reject("Order must be paid before completion");
    }

    private boolean isTerminal(OrderStatus status) {
        return status == OrderStatus.COMPLETED
            || status == OrderStatus.CANCELLED;
    }

    private void reject() { throw new BadRequestException("Invalid order flow status"); }

    private void reject(String message) { throw new BadRequestException(message); }

}
