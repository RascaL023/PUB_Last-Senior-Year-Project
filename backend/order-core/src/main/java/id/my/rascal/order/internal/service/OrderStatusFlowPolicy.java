package id.my.rascal.order.internal.service;

import org.springframework.stereotype.Service;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.order.internal.model.enums.OrderStatus;

@Service
public class OrderStatusFlowPolicy {

    public void validateFlow(
        OrderStatus oldStatus,
        OrderStatus newStatus
    ) {
        if (oldStatus.equals(OrderStatus.CANCELLED)) reject("Order has been cancelled");
        else if (newStatus.equals(OrderStatus.CANCELLED) && !oldStatus.equals(OrderStatus.CREATED)) 
            reject("Order with status " + oldStatus + " cannot be cancelled");
        else if (oldStatus.equals(OrderStatus.CREATED) && newStatus.equals(OrderStatus.CANCELLED)) return;
        else if (oldStatus.equals(OrderStatus.CREATED) && !newStatus.equals(OrderStatus.PREPARING)) reject();
        else if (oldStatus.equals(OrderStatus.PREPARING) && !newStatus.equals(OrderStatus.READY)) reject();
        else if (oldStatus.equals(OrderStatus.READY) && !newStatus.equals(OrderStatus.COMPLETED)) reject();
    }

    private void reject() {
        throw new BadRequestException("Invalid order flow status");
    }

    private void reject(String message) {
        throw new BadRequestException(message);
    }

}
