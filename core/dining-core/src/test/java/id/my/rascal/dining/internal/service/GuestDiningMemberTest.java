package id.my.rascal.dining.internal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.common.exception.ForbiddenException;
import id.my.rascal.customer.api.CustomerApi;
import id.my.rascal.customer.api.CustomerApiResponse;
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
import id.my.rascal.order.api.OrderApi;
import id.my.rascal.order.api.OrderApiResponse;
import id.my.rascal.order.api.OrderTypeApiResponse;

class GuestDiningMemberTest {

    private static final String TOKEN = "member-guest-token";
    private static final Long DINING_ID = 30L;
    private static final Long TABLE_ID = 2L;
    private static final Long USER_AUTH_ID = 55L;
    private static final Long CUSTOMER_ID = 77L;

    private DiningRepository diningRepository;
    private DiningOrderRepository diningOrderRepository;
    private TableService tableService;
    private OrderApi orderApi;
    private InvoiceApi invoiceApi;
    private CustomerApi customerApi;
    private GuestDiningService guestDiningService;

    @BeforeEach
    void setUp() {
        diningRepository = mock(DiningRepository.class);
        diningOrderRepository = mock(DiningOrderRepository.class);
        tableService = mock(TableService.class);
        orderApi = mock(OrderApi.class);
        invoiceApi = mock(InvoiceApi.class);
        customerApi = mock(CustomerApi.class);

        DiningService diningService = new DiningService(
            diningRepository, diningOrderRepository, mock(DiningTableRepository.class),
            tableService, orderApi, invoiceApi,
            new DiningEventPublisherService(mock(ApplicationEventPublisher.class)),
            new GuestTokenGenerator(),
            customerApi
        );
        guestDiningService = new GuestDiningService(
            diningRepository, diningService, orderApi, invoiceApi, customerApi
        );
    }

    @Test
    void addOrderAsMember_attachesCustomerIdFromProfile() {
        stubOpenSession();
        stubMember(CUSTOMER_ID, "Budi Santoso");
        when(diningOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(orderApi.createOrder(any())).thenReturn(order(101L, 20000));
        when(orderApi.getOrderItems(101L)).thenReturn(List.of());
        when(diningOrderRepository.findOrderIdsByDiningId(DINING_ID)).thenReturn(List.of(101L));
        when(orderApi.getOrders(List.of(101L))).thenReturn(List.of(order(101L, 20000)));
        when(invoiceApi.getDiningInvoice(DINING_ID)).thenReturn(null);

        GuestDiningResponse response = guestDiningService.addOrderAsMember(TOKEN, USER_AUTH_ID,
            new GuestOrderRequest(null, null, List.of(new DiningOrderItemRequest(1L, 2, List.of()))));

        ArgumentCaptor<id.my.rascal.order.api.OrderApiCreateRequest> captor =
            ArgumentCaptor.forClass(id.my.rascal.order.api.OrderApiCreateRequest.class);
        verify(orderApi).createOrder(captor.capture());
        assertEquals(CUSTOMER_ID, captor.getValue().customerId());
        assertEquals("Budi Santoso", captor.getValue().customerName());
        assertEquals(20000, response.totalPrice());
    }

    @Test
    void addOrderAsMember_bodyNameOverridesProfileName() {
        stubOpenSession();
        stubMember(CUSTOMER_ID, "Budi Santoso");
        when(diningOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(orderApi.createOrder(any())).thenReturn(order(101L, 20000));
        when(orderApi.getOrderItems(101L)).thenReturn(List.of());
        when(diningOrderRepository.findOrderIdsByDiningId(DINING_ID)).thenReturn(List.of(101L));
        when(orderApi.getOrders(List.of(101L))).thenReturn(List.of(order(101L, 20000)));
        when(invoiceApi.getDiningInvoice(DINING_ID)).thenReturn(null);

        guestDiningService.addOrderAsMember(TOKEN, USER_AUTH_ID,
            new GuestOrderRequest("Budi +1", null, List.of(new DiningOrderItemRequest(1L, 2, List.of()))));

        ArgumentCaptor<id.my.rascal.order.api.OrderApiCreateRequest> captor =
            ArgumentCaptor.forClass(id.my.rascal.order.api.OrderApiCreateRequest.class);
        verify(orderApi).createOrder(captor.capture());
        assertEquals(CUSTOMER_ID, captor.getValue().customerId());
        assertEquals("Budi +1", captor.getValue().customerName());
    }

    @Test
    void addOrderAsMember_accountWithoutProfile_forbidden() {
        stubOpenSession();
        when(customerApi.getByUserAuthId(USER_AUTH_ID)).thenReturn(Optional.empty());

        ForbiddenException thrown = assertThrows(ForbiddenException.class,
            () -> guestDiningService.addOrderAsMember(TOKEN, USER_AUTH_ID,
                new GuestOrderRequest(null, null, List.of(new DiningOrderItemRequest(1L, 1, List.of())))));

        assertTrue(thrown.getMessage().contains("Customer profile not found"));
        verify(orderApi, never()).createOrder(any());
    }

    @Test
    void addOrderAsMember_closedSession_rejectedBeforeProfileLookup() {
        stubSession(DiningStatus.CLOSED);
        stubMember(CUSTOMER_ID, "Budi Santoso");

        assertThrows(BadRequestException.class,
            () -> guestDiningService.addOrderAsMember(TOKEN, USER_AUTH_ID,
                new GuestOrderRequest(null, null, List.of(new DiningOrderItemRequest(1L, 1, List.of())))));

        verify(customerApi, never()).getByUserAuthId(any());
        verify(orderApi, never()).createOrder(any());
    }

    private void stubOpenSession() {
        stubSession(DiningStatus.OPEN);
        when(customerApi.existsById(CUSTOMER_ID)).thenReturn(true);
    }

    private void stubSession(DiningStatus status) {
        Dining dining = new Dining();
        dining.setId(DINING_ID);
        dining.setTableId(TABLE_ID);
        dining.setGuestToken(TOKEN);
        dining.setStatus(status);
        when(diningRepository.findByGuestToken(TOKEN)).thenReturn(Optional.of(dining));
        when(diningRepository.findById(DINING_ID)).thenReturn(Optional.of(dining));

        DiningTable table = new DiningTable();
        table.setId(TABLE_ID);
        table.setTableNumber("T-02");
        table.setStatus(TableStatus.OCCUPIED);
        when(tableService.findActive(TABLE_ID)).thenReturn(table);
    }

    private void stubMember(Long customerId, String name) {
        when(customerApi.getByUserAuthId(USER_AUTH_ID))
            .thenReturn(Optional.of(CustomerApiResponse.of(customerId, USER_AUTH_ID, name, null, null)));
    }

    private OrderApiResponse order(Long id, int totalPrice) {
        return new OrderApiResponse(
            id, OrderTypeApiResponse.DINE_IN, "ORD-" + id, "CREATED",
            CUSTOMER_ID, "Budi", totalPrice, java.time.LocalDateTime.now()
        );
    }

}
