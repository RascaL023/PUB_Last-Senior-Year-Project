package id.my.rascal.dining.internal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.dining.api.event.DiningOrderAddedEvent;
import id.my.rascal.dining.internal.entity.Dining;
import id.my.rascal.dining.internal.entity.DiningTable;
import id.my.rascal.dining.internal.model.request.CreateDiningOrderRequest;
import id.my.rascal.dining.internal.model.request.DiningOrderItemRequest;
import id.my.rascal.dining.internal.repository.DiningOrderRepository;
import id.my.rascal.dining.internal.repository.DiningRepository;
import id.my.rascal.dining.internal.repository.DiningTableRepository;
import id.my.rascal.invoice.api.InvoiceApi;
import id.my.rascal.invoice.api.InvoiceApiResponse;
import id.my.rascal.order.api.OrderApi;
import id.my.rascal.order.api.OrderApiResponse;
import id.my.rascal.order.api.OrderTypeApiResponse;
import id.my.rascal.order.api.event.dto.OrderItemSnapshot;

class DiningServiceEventTest {

    @Test
    void addOrder_eventCarriesSameHeaderFactsAsCreatedOrder() {
        DiningRepository diningRepository = mock(DiningRepository.class);
        DiningOrderRepository diningOrderRepository = mock(DiningOrderRepository.class);
        TableService tableService = mock(TableService.class);
        OrderApi orderApi = mock(OrderApi.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        when(diningOrderRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        Dining dining = new Dining();
        dining.setId(20L);
        dining.setTableId(1L);
        dining.markOpen();
        when(diningRepository.findById(20L)).thenReturn(Optional.of(dining));

        DiningTable table = new DiningTable();
        table.setTableNumber("1");
        when(tableService.findActive(1L)).thenReturn(table);

        LocalDateTime orderCreatedAt = LocalDateTime.now();
        OrderApiResponse created = new OrderApiResponse(
            101L, OrderTypeApiResponse.DINE_IN, "ORD-08092026-AAAAAA",
            "CREATED", null, "Budi", 50000, orderCreatedAt
        );
        when(orderApi.createOrder(any())).thenReturn(created);
        when(orderApi.getOrderItems(101L)).thenReturn(List.of(
            new OrderItemSnapshot(1L, 10L, "Nasi Goreng", 2, 25000, 50000)
        ));
        when(diningOrderRepository.findOrderIdsByDiningId(20L)).thenReturn(List.of(101L));
        when(orderApi.getOrders(List.of(101L))).thenReturn(List.of(created));

        DiningService diningService = new DiningService(
            diningRepository, diningOrderRepository, mock(DiningTableRepository.class),
            tableService, orderApi, mock(InvoiceApi.class), new DiningEventPublisherService(eventPublisher)
        );

        diningService.addOrder(20L, new CreateDiningOrderRequest(
            null, "Budi", null,
            List.of(new DiningOrderItemRequest(1L, 2, List.of()))
        ));

        ArgumentCaptor<DiningOrderAddedEvent> event = ArgumentCaptor.forClass(DiningOrderAddedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());

        assertEquals(20L, event.getValue().diningId());
        assertEquals(101L, event.getValue().orderId());
        assertEquals("ORD-08092026-AAAAAA", event.getValue().orderNumber());
        assertEquals(50000, event.getValue().totalAmount());
        assertEquals(orderCreatedAt, event.getValue().createdAt());
        assertEquals(1, event.getValue().items().size());
        assertEquals(1L, event.getValue().items().get(0).orderItemId());
    }

    @Test
    void addOrder_toSettledInvoice_failsBeforeOrderCreated() {
        DiningRepository diningRepository = mock(DiningRepository.class);
        OrderApi orderApi = mock(OrderApi.class);
        InvoiceApi invoiceApi = mock(InvoiceApi.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);

        Dining dining = new Dining();
        dining.setId(20L);
        dining.setTableId(1L);
        dining.markOpen();
        when(diningRepository.findById(20L)).thenReturn(Optional.of(dining));

        when(invoiceApi.getDiningInvoice(20L)).thenReturn(new InvoiceApiResponse(
            900L, "INV-08092026-AAAAAA", 20L, "PAID", 80000, 80000, 0,
            LocalDateTime.now(), LocalDateTime.now(), List.of()
        ));

        DiningService diningService = new DiningService(
            diningRepository, mock(DiningOrderRepository.class), mock(DiningTableRepository.class),
            mock(TableService.class), orderApi, invoiceApi, new DiningEventPublisherService(eventPublisher)
        );

        BadRequestException thrown = assertThrows(BadRequestException.class, () ->
            diningService.addOrder(20L, new CreateDiningOrderRequest(
                null, "Budi", null,
                List.of(new DiningOrderItemRequest(1L, 2, List.of()))
            ))
        );

        assertTrue(thrown.getMessage().contains("INV-08092026-AAAAAA"));
        verify(orderApi, never()).createOrder(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

}
