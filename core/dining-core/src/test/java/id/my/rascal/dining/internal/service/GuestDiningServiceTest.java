package id.my.rascal.dining.internal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.common.exception.NotFoundException;
import id.my.rascal.customer.api.CustomerApi;
import id.my.rascal.dining.internal.entity.Dining;
import id.my.rascal.dining.internal.entity.DiningStatus;
import id.my.rascal.dining.internal.entity.DiningTable;
import id.my.rascal.dining.internal.entity.TableStatus;
import id.my.rascal.dining.internal.model.request.DiningOrderItemRequest;
import id.my.rascal.dining.internal.model.request.GuestOrderRequest;
import id.my.rascal.dining.internal.model.response.GuestDiningResponse;
import id.my.rascal.dining.internal.repository.DiningOrderRepository;
import id.my.rascal.dining.internal.repository.DiningRepository;
import id.my.rascal.dining.internal.repository.DiningTableRepository;
import id.my.rascal.invoice.api.InvoiceApi;
import id.my.rascal.invoice.api.InvoiceApiResponse;
import id.my.rascal.order.api.OrderApi;
import id.my.rascal.order.api.OrderApiCreateRequest;
import id.my.rascal.order.api.OrderApiResponse;
import id.my.rascal.order.api.OrderTypeApiResponse;

class GuestDiningServiceTest {

    private static final String TOKEN = "test-guest-token";
    private static final Long DINING_ID = 20L;
    private static final Long TABLE_ID = 1L;

    private DiningRepository diningRepository;
    private DiningOrderRepository diningOrderRepository;
    private DiningTableRepository diningTableRepository;
    private TableService tableService;
    private OrderApi orderApi;
    private InvoiceApi invoiceApi;
    private CustomerApi customerApi;
    private DiningService diningService;
    private GuestDiningService guestDiningService;

    @BeforeEach
    void setUp() {
        diningRepository = mock(DiningRepository.class);
        diningOrderRepository = mock(DiningOrderRepository.class);
        diningTableRepository = mock(DiningTableRepository.class);
        tableService = mock(TableService.class);
        orderApi = mock(OrderApi.class);
        invoiceApi = mock(InvoiceApi.class);
        customerApi = mock(CustomerApi.class);
        diningService = new DiningService(
            diningRepository, diningOrderRepository, diningTableRepository,
            tableService, orderApi, invoiceApi,
            new DiningEventPublisherService(mock(ApplicationEventPublisher.class)),
            new GuestTokenGenerator(),
            customerApi
        );
        guestDiningService = new GuestDiningService(
            diningRepository, diningOrderRepository, diningTableRepository,
            diningService, orderApi, invoiceApi, customerApi
        );
        when(orderApi.getItemsByOrderIds(any())).thenReturn(Map.of());
    }

    // ── getByToken ────────────────────────────────────────────────────────

    @Test
    void getByToken_unknownToken_throwsGenericNotFound() {
        when(diningRepository.findByGuestToken("nope")).thenReturn(Optional.empty());

        NotFoundException thrown = assertThrows(NotFoundException.class,
            () -> guestDiningService.getByToken("nope"));
        assertEquals("Session not found", thrown.getMessage());
    }

    @Test
    void getByToken_closedSession_stillReadable() {
        Dining closed = stubDining(DiningStatus.CLOSED);
        stubOrders(closed, List.of());
        when(invoiceApi.getDiningInvoice(DINING_ID)).thenReturn(null);

        GuestDiningResponse response = guestDiningService.getByToken(TOKEN);

        assertEquals("CLOSED", response.status());
    }

    @Test
    void getByToken_openSessionWithoutInvoice_invoiceStatusNull() {
        Dining dining = stubDining(DiningStatus.OPEN);
        stubOrders(dining, List.of());
        when(invoiceApi.getDiningInvoice(DINING_ID)).thenReturn(null);

        GuestDiningResponse response = guestDiningService.getByToken(TOKEN);

        assertEquals("OPEN", response.status());
        assertEquals("T-01", response.tableNumber());
        assertNull(response.invoiceStatus());
    }

    @Test
    void getByToken_invoiceStatusMapped() {
        Dining dining = stubDining(DiningStatus.OPEN);
        stubOrders(dining, List.of());
        when(invoiceApi.getDiningInvoice(DINING_ID)).thenReturn(invoice("PARTIALLY_PAID"));

        GuestDiningResponse response = guestDiningService.getByToken(TOKEN);

        assertEquals("PARTIALLY_PAID", response.invoiceStatus());
    }

    @Test
    void getByToken_totalPriceExcludesCancelledOrders() {
        Dining dining = stubDining(DiningStatus.OPEN);
        stubOrders(dining, List.of(
            order(101L, "COMPLETED", 50000),
            order(102L, "CANCELLED", 15000)
        ));
        when(invoiceApi.getDiningInvoice(DINING_ID)).thenReturn(null);

        GuestDiningResponse response = guestDiningService.getByToken(TOKEN);

        assertEquals(50000, response.totalPrice());
        assertEquals(2, response.orders().size());
    }

    // ── getByCode ─────────────────────────────────────────────────────────

    @Test
    void getByCode_onlyResolvesOpenSessions() {
        when(diningRepository.findByGuestCodeAndStatus("123456", DiningStatus.OPEN))
            .thenReturn(Optional.empty());

        NotFoundException thrown = assertThrows(NotFoundException.class,
            () -> guestDiningService.getByCode("123456"));
        assertEquals("Session not found", thrown.getMessage());
    }

    // ── addOrder ──────────────────────────────────────────────────────────

    @Test
    void addOrder_delegatesWithNullCustomerId_andReturnsFreshState() {
        Dining dining = stubDining(DiningStatus.OPEN);
        when(diningOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        stubOrders(dining, List.of());
        when(invoiceApi.getDiningInvoice(DINING_ID)).thenReturn(null);

        OrderApiResponse created = new OrderApiResponse(
            101L, OrderTypeApiResponse.DINE_IN, "ORD-1", "CREATED",
            null, "Budi", 20000, LocalDateTime.now()
        );
        when(orderApi.createOrder(any())).thenReturn(created);
        when(orderApi.getOrderItems(101L)).thenReturn(List.of());
        when(diningOrderRepository.findOrderIdsByDiningId(DINING_ID)).thenReturn(List.of(101L));
        when(orderApi.getOrders(List.of(101L))).thenReturn(List.of(created));

        GuestDiningResponse response = guestDiningService.addOrder(TOKEN,
            new GuestOrderRequest("Budi", "Extra es",
                List.of(new DiningOrderItemRequest(1L, 2, List.of()))));

        ArgumentCaptor<OrderApiCreateRequest> captor =
            ArgumentCaptor.forClass(OrderApiCreateRequest.class);
        verify(orderApi).createOrder(captor.capture());
        assertNull(captor.getValue().customerId(), "customerId must never come from guest body");
        assertEquals("Budi", captor.getValue().customerName());

        assertEquals(20000, response.totalPrice());
        assertEquals(1, response.orders().size());
    }

    @Test
    void addOrder_toClosedSession_rejectedBeforeDelegation() {
        stubDining(DiningStatus.CLOSED);

        BadRequestException thrown = assertThrows(BadRequestException.class,
            () -> guestDiningService.addOrder(TOKEN,
                new GuestOrderRequest(null, null,
                    List.of(new DiningOrderItemRequest(1L, 1, List.of())))));

        assertTrue(thrown.getMessage().contains("closed"));
        verify(orderApi, never()).createOrder(any());
    }

    @Test
    void addOrder_withPaidInvoice_rejectedViaDelegatedGuard() {
        stubDining(DiningStatus.OPEN);
        when(invoiceApi.getDiningInvoice(DINING_ID)).thenReturn(invoice("PAID"));

        BadRequestException thrown = assertThrows(BadRequestException.class,
            () -> guestDiningService.addOrder(TOKEN,
                new GuestOrderRequest(null, null,
                    List.of(new DiningOrderItemRequest(1L, 1, List.of())))));

        assertTrue(thrown.getMessage().contains("lunas"));
        verify(orderApi, never()).createOrder(any());
    }

    // ── helpers ───────────────────────────────────────────────────────────

    private Dining stubDining(DiningStatus status) {
        Dining dining = new Dining();
        dining.setId(DINING_ID);
        dining.setTableId(TABLE_ID);
        dining.setGuestToken(TOKEN);
        dining.setGuestCode("123456");
        dining.setStatus(status);
        when(diningRepository.findByGuestToken(TOKEN)).thenReturn(Optional.of(dining));
        when(diningRepository.findById(DINING_ID)).thenReturn(Optional.of(dining));
        return dining;
    }

    private void stubOrders(Dining dining, List<OrderApiResponse> orders) {
        List<Long> ids = orders.stream().map(OrderApiResponse::id).toList();
        when(diningOrderRepository.findOrderIdsByDiningId(DINING_ID)).thenReturn(ids);
        when(orderApi.getOrders(ids.isEmpty() ? List.of() : ids)).thenReturn(orders);

        DiningTable table = new DiningTable();
        table.setId(TABLE_ID);
        table.setTableNumber("T-01");
        table.setStatus(TableStatus.OCCUPIED);
        when(tableService.findActive(TABLE_ID)).thenReturn(table);
    }

    private OrderApiResponse order(Long id, String status, int totalPrice) {
        return new OrderApiResponse(
            id, OrderTypeApiResponse.DINE_IN, "ORD-" + id, status,
            null, null, totalPrice, LocalDateTime.now()
        );
    }

    private InvoiceApiResponse invoice(String status) {
        return new InvoiceApiResponse(
            900L, "INV-1", DINING_ID, status, 50000, 0, 50000,
            LocalDateTime.now(), LocalDateTime.now(), null, null, List.of()
        );
    }

}
