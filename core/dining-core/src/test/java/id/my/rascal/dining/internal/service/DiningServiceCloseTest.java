package id.my.rascal.dining.internal.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.dining.internal.entity.Dining;
import id.my.rascal.dining.internal.entity.DiningTable;
import id.my.rascal.dining.internal.entity.TableStatus;
import id.my.rascal.dining.internal.repository.DiningOrderRepository;
import id.my.rascal.dining.internal.repository.DiningRepository;
import id.my.rascal.dining.internal.repository.DiningTableRepository;
import id.my.rascal.invoice.api.InvoiceApi;
import id.my.rascal.invoice.api.InvoiceApiResponse;
import id.my.rascal.order.api.OrderApi;
import id.my.rascal.order.api.OrderApiResponse;
import id.my.rascal.order.api.OrderTypeApiResponse;

class DiningServiceCloseTest {

    private DiningRepository diningRepository;
    private DiningOrderRepository diningOrderRepository;
    private TableService tableService;
    private OrderApi orderApi;
    private InvoiceApi invoiceApi;
    private DiningService diningService;

    @BeforeEach
    void setUp() {
        diningRepository = mock(DiningRepository.class);
        diningOrderRepository = mock(DiningOrderRepository.class);
        tableService = mock(TableService.class);
        orderApi = mock(OrderApi.class);
        invoiceApi = mock(InvoiceApi.class);
        diningService = new DiningService(
            diningRepository, diningOrderRepository, mock(DiningTableRepository.class),
            tableService, orderApi, invoiceApi,
            new DiningEventPublisherService(mock(ApplicationEventPublisher.class))
        );
    }

    @Test
    void closeDining_withOpenInvoice_rejected() {
        stubOpenDining(3L, 1L);
        when(diningOrderRepository.findOrderIdsByDiningId(3L)).thenReturn(List.of(101L));
        when(orderApi.getOrders(List.of(101L))).thenReturn(List.of(completedOrder(101L)));
        when(invoiceApi.getDiningInvoice(3L)).thenReturn(openInvoice(3L));

        BadRequestException thrown = assertThrows(BadRequestException.class, () -> diningService.close(3L));
        assertTrue(thrown.getMessage().contains("Tagihan belum lunas"));
        verify(diningRepository, never()).save(any());
    }

    @Test
    void closeDining_afterInvoicePaid_allowed() {
        Dining dining = stubOpenDining(3L, 1L);
        when(diningOrderRepository.findOrderIdsByDiningId(3L)).thenReturn(List.of(101L));
        when(orderApi.getOrders(List.of(101L))).thenReturn(List.of(completedOrder(101L)));
        when(invoiceApi.getDiningInvoice(3L)).thenReturn(new InvoiceApiResponse(
            900L, "INV-1", 3L, "PAID", 50000, 50000, 0,
            LocalDateTime.now(), LocalDateTime.now(), List.of()
        ));
        when(diningRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> diningService.close(3L));
        verify(diningRepository).save(dining);
    }

    @Test
    void closeDining_withNoOrders_allowed() {
        Dining dining = stubOpenDining(3L, 1L);
        when(diningOrderRepository.findOrderIdsByDiningId(3L)).thenReturn(List.of());
        when(orderApi.getOrders(List.of())).thenReturn(List.of());
        when(invoiceApi.getDiningInvoice(3L)).thenReturn(null);
        when(diningRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> diningService.close(3L));
        verify(diningRepository).save(dining);
    }

    @Test
    void closeDining_afterVoid_allowed() {
        Dining dining = stubOpenDining(3L, 1L);
        when(diningOrderRepository.findOrderIdsByDiningId(3L)).thenReturn(List.of(101L));
        when(orderApi.getOrders(List.of(101L))).thenReturn(List.of(completedOrder(101L)));
        when(invoiceApi.getDiningInvoice(3L)).thenReturn(new InvoiceApiResponse(
            900L, "INV-1", 3L, "VOID", 50000, 0, 0,
            LocalDateTime.now(), LocalDateTime.now(), List.of()
        ));
        when(diningRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> diningService.close(3L));
        verify(diningRepository).save(dining);
    }

    private Dining stubOpenDining(Long diningId, Long tableId) {
        Dining dining = new Dining();
        dining.setId(diningId);
        dining.setTableId(tableId);
        dining.markOpen();
        when(diningRepository.findById(diningId)).thenReturn(Optional.of(dining));

        DiningTable table = new DiningTable();
        table.setId(tableId);
        table.setTableNumber("T1");
        table.setStatus(TableStatus.OCCUPIED);
        when(tableService.findActive(tableId)).thenReturn(table);
        return dining;
    }

    private OrderApiResponse completedOrder(Long id) {
        return new OrderApiResponse(
            id, OrderTypeApiResponse.DINE_IN, "ORD-1",
            "COMPLETED", null, null, 50000, LocalDateTime.now()
        );
    }

    private InvoiceApiResponse openInvoice(Long diningId) {
        return new InvoiceApiResponse(
            900L, "INV-OPEN", diningId, "OPEN", 50000, 0, 50000,
            LocalDateTime.now(), LocalDateTime.now(), List.of()
        );
    }
}
