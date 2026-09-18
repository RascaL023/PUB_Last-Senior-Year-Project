package id.my.rascal.dining.internal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import id.my.rascal.common.exception.ForbiddenException;
import id.my.rascal.customer.api.CustomerApi;
import id.my.rascal.customer.api.CustomerApiResponse;
import id.my.rascal.dining.internal.entity.Dining;
import id.my.rascal.dining.internal.entity.DiningStatus;
import id.my.rascal.dining.internal.entity.DiningTable;
import id.my.rascal.dining.internal.model.response.GuestDiningResponse;
import id.my.rascal.dining.internal.model.response.MyDiningResponse;
import id.my.rascal.dining.internal.repository.DiningOrderRepository;
import id.my.rascal.dining.internal.repository.DiningRepository;
import id.my.rascal.dining.internal.repository.DiningTableRepository;
import id.my.rascal.invoice.api.InvoiceApi;
import id.my.rascal.invoice.api.InvoiceApiResponse;
import id.my.rascal.order.api.OrderApi;
import id.my.rascal.order.api.OrderApiResponse;
import id.my.rascal.order.api.OrderItemDetail;
import id.my.rascal.order.api.OrderTypeApiResponse;

class GuestDiningTrackingTest {

    private static final String TOKEN = "tracking-token";
    private static final Long DINING_ID = 20L;
    private static final Long TABLE_ID = 1L;
    private static final Long ORDER_ID = 101L;
    private static final Long USER_AUTH_ID = 55L;
    private static final Long CUSTOMER_ID = 77L;

    private DiningRepository diningRepository;
    private DiningOrderRepository diningOrderRepository;
    private DiningTableRepository diningTableRepository;
    private DiningService diningService;
    private OrderApi orderApi;
    private InvoiceApi invoiceApi;
    private CustomerApi customerApi;
    private GuestDiningService guestDiningService;

    @BeforeEach
    void setUp() {
        diningRepository = mock(DiningRepository.class);
        diningOrderRepository = mock(DiningOrderRepository.class);
        diningTableRepository = mock(DiningTableRepository.class);
        diningService = mock(DiningService.class);
        orderApi = mock(OrderApi.class);
        invoiceApi = mock(InvoiceApi.class);
        customerApi = mock(CustomerApi.class);
        guestDiningService = new GuestDiningService(
            diningRepository, diningOrderRepository, diningTableRepository,
            diningService, orderApi, invoiceApi, customerApi
        );
    }

    @Test
    void getByToken_exposesOrderIdentityAndItemsInOneBatch() {
        stubSessionByToken(dining(DINING_ID, TABLE_ID, DiningStatus.OPEN));
        when(diningService.getOrderIds(DINING_ID)).thenReturn(List.of(ORDER_ID));
        when(diningService.getTableNumber(TABLE_ID)).thenReturn("T-01");
        when(orderApi.getOrders(List.of(ORDER_ID))).thenReturn(List.of(order()));
        when(orderApi.getItemsByOrderIds(List.of(ORDER_ID)))
            .thenReturn(Map.of(ORDER_ID, List.of(itemDetail())));

        GuestDiningResponse response = guestDiningService.getByToken(TOKEN);

        assertEquals("ORD-20260918-001", response.orders().get(0).orderNumber());
        assertEquals(1, response.orders().get(0).items().size());
        assertEquals("Nasi Goreng", response.orders().get(0).items().get(0).itemName());
        assertEquals(1, response.orders().get(0).items().get(0).modifiers().size());
        verify(orderApi, times(1)).getItemsByOrderIds(any());
    }

    @Test
    void findMySessions_withoutMemberProfile_isForbidden() {
        when(customerApi.getByUserAuthId(USER_AUTH_ID)).thenReturn(Optional.empty());

        assertThrows(
            ForbiddenException.class,
            () -> guestDiningService.findMySessions(USER_AUTH_ID, null)
        );
    }

    @Test
    void findMySessions_defaultsToOpenAndScopesToMemberOrders() {
        when(customerApi.getByUserAuthId(USER_AUTH_ID)).thenReturn(Optional.of(member()));
        when(orderApi.findOrderIdsByCustomerId(CUSTOMER_ID)).thenReturn(List.of(ORDER_ID));
        when(diningOrderRepository.findDiningIdsByOrderIds(List.of(ORDER_ID))).thenReturn(List.of(DINING_ID));
        when(diningRepository.findAllByIds(List.of(DINING_ID)))
            .thenReturn(List.of(dining(DINING_ID, TABLE_ID, DiningStatus.OPEN)));
        when(diningOrderRepository.findOrderIdsGroupedByDiningId(List.of(DINING_ID)))
            .thenReturn(Map.of(DINING_ID, List.of(ORDER_ID)));
        when(orderApi.getOrders(List.of(ORDER_ID))).thenReturn(List.of(order()));
        when(orderApi.getItemsByOrderIds(List.of(ORDER_ID))).thenReturn(Map.of());
        when(diningTableRepository.findActiveByIds(List.of(TABLE_ID))).thenReturn(List.of(table()));
        when(invoiceApi.getDiningInvoice(DINING_ID)).thenReturn(invoice());

        List<MyDiningResponse> sessions = guestDiningService.findMySessions(USER_AUTH_ID, null);

        assertEquals(1, sessions.size());
        assertEquals(DINING_ID, sessions.get(0).diningId());
        assertEquals(TOKEN, sessions.get(0).guestToken());
        assertEquals("T-01", sessions.get(0).tableNumber());
        assertEquals("OPEN", sessions.get(0).status());
        assertEquals("OPEN", sessions.get(0).invoiceStatus());
        assertEquals(30000, sessions.get(0).totalPrice());
        assertEquals("ORD-20260918-001", sessions.get(0).orders().get(0).orderNumber());
    }

    @Test
    void findMySessions_explicitClosedStatus_filtersOutOpenSessions() {
        when(customerApi.getByUserAuthId(USER_AUTH_ID)).thenReturn(Optional.of(member()));
        when(orderApi.findOrderIdsByCustomerId(CUSTOMER_ID)).thenReturn(List.of(ORDER_ID));
        when(diningOrderRepository.findDiningIdsByOrderIds(List.of(ORDER_ID))).thenReturn(List.of(DINING_ID));
        when(diningRepository.findAllByIds(List.of(DINING_ID)))
            .thenReturn(List.of(dining(DINING_ID, TABLE_ID, DiningStatus.OPEN)));

        assertEquals(List.of(), guestDiningService.findMySessions(USER_AUTH_ID, DiningStatus.CLOSED));
        verify(diningOrderRepository, never()).findOrderIdsGroupedByDiningId(any());
    }

    @Test
    void findMySessions_withoutOrders_returnsEmptyWithoutQueryingDinings() {
        when(customerApi.getByUserAuthId(USER_AUTH_ID)).thenReturn(Optional.of(member()));
        when(orderApi.findOrderIdsByCustomerId(CUSTOMER_ID)).thenReturn(List.of());

        assertEquals(List.of(), guestDiningService.findMySessions(USER_AUTH_ID, null));
        verify(diningRepository, never()).findAllByIds(any());
    }

    private void stubSessionByToken(Dining dining) {
        when(diningRepository.findByGuestToken(TOKEN)).thenReturn(Optional.of(dining));
    }

    private Dining dining(Long id, Long tableId, DiningStatus status) {
        Dining dining = new Dining();
        dining.setId(id);
        dining.setTableId(tableId);
        dining.setGuestToken(TOKEN);
        dining.setGuestCode("123456");
        dining.setStatus(status);
        dining.setCreatedAt(LocalDateTime.now());
        return dining;
    }

    private DiningTable table() {
        DiningTable table = new DiningTable();
        table.setId(TABLE_ID);
        table.setTableNumber("T-01");
        return table;
    }

    private OrderApiResponse order() {
        return new OrderApiResponse(
            ORDER_ID,
            OrderTypeApiResponse.DINE_IN,
            "ORD-20260918-001",
            "PREPARING",
            null,
            "Budi",
            30000,
            LocalDateTime.now()
        );
    }

    private OrderItemDetail itemDetail() {
        return new OrderItemDetail(
            1L,
            1L,
            "Nasi Goreng",
            15000,
            2,
            30000,
            List.of(new OrderItemDetail.Modifier(13L, "Regular", 0))
        );
    }

    private InvoiceApiResponse invoice() {
        return new InvoiceApiResponse(
            1L, "INV-001", DINING_ID, "OPEN",
            30000, 0, 30000,
            LocalDateTime.now(), LocalDateTime.now(),
            null, null, List.of()
        );
    }

    private CustomerApiResponse member() {
        return new CustomerApiResponse(CUSTOMER_ID, USER_AUTH_ID, "Budi", "budi@rascal.id", "0812");
    }

}
