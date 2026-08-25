package id.my.rascal.order.internal.service;

import org.springframework.stereotype.Service;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.order.internal.model.enums.OrderPaidStatus;
import id.my.rascal.order.internal.model.enums.OrderStatus;
import id.my.rascal.order.internal.model.enums.OrderType;

@Service
public class OrderStatusFlowPolicy {

    public void validateFlow(
        OrderStatus oldStatus,
        OrderStatus newStatus
    ) {
        if (isTerminal(oldStatus)) reject("Order with status " + oldStatus + " can't be changed");
        else if (newStatus.equals(OrderStatus.CANCELLED) && !oldStatus.equals(OrderStatus.CREATED)) 
            reject("Order with status " + oldStatus + " cannot be cancelled");
        else if (oldStatus.equals(OrderStatus.CREATED) && newStatus.equals(OrderStatus.CANCELLED)) return;
        else if (oldStatus.equals(OrderStatus.CREATED) && !newStatus.equals(OrderStatus.PREPARING)) reject();
        else if (oldStatus.equals(OrderStatus.PREPARING) && !newStatus.equals(OrderStatus.READY)) reject();
        else if (oldStatus.equals(OrderStatus.READY) && !newStatus.equals(OrderStatus.COMPLETED)) reject();
    }

    public void validateTransitionRequirements(
        OrderType type,
        OrderPaidStatus paidStatus,
        OrderStatus nextStatus
    ) {
        switch (type) {
            case TAKEAWAY -> {
                if (nextStatus.equals(OrderStatus.PREPARING) && paidStatus.equals(OrderPaidStatus.UNAPAID))
                    reject("Order with type " + type + " must be paid first to be proceed");
            }

            default -> reject("Invalid order type");
        }
    }

    private boolean isTerminal(OrderStatus oldStatus) {
        return oldStatus == OrderStatus.COMPLETED
            || oldStatus == OrderStatus.CANCELLED;
    }

    private void reject() {
        throw new BadRequestException("Invalid order flow status");
    }

    private void reject(String message) {
        throw new BadRequestException(message);
    }

}
