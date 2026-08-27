package id.my.rascal.dining.internal.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.common.exception.NotFoundException;
import id.my.rascal.dining.internal.entity.Dining;
import id.my.rascal.dining.internal.entity.DiningStatus;
import id.my.rascal.dining.internal.entity.DiningTable;
import id.my.rascal.dining.internal.entity.TableStatus;
import id.my.rascal.dining.internal.model.request.CreateDiningOrderRequest;
import id.my.rascal.dining.internal.model.request.OpenDiningRequest;
import id.my.rascal.dining.internal.model.response.DiningResponse;
import id.my.rascal.dining.internal.repository.DiningRepository;
import id.my.rascal.order.api.OrderApiResponse;
import id.my.rascal.order.api.OrderApi;
import id.my.rascal.order.api.OrderApiCreateRequest;
import id.my.rascal.order.api.OrderItemApiRequest;
import id.my.rascal.order.api.OrderTypeApiResponse;

@ExtendWith(MockitoExtension.class)
class DiningServiceTest {

    @Mock
    private DiningRepository diningRepository;

    @Mock
    private TableService tableService;

    @Mock
    private OrderApi orderApi;

    @InjectMocks
    private DiningService diningService;

    private DiningTable availableTable;
    private DiningTable occupiedTable;
    private Dining openDining;
    private Dining closedDining;

    @BeforeEach
    void setUp() {
        availableTable = new DiningTable();
        availableTable.setId(1L);
        availableTable.setTableNumber("1");
        availableTable.setStatus(TableStatus.AVAILABLE);
        availableTable.setCreatedAt(LocalDateTime.now());

        occupiedTable = new DiningTable();
        occupiedTable.setId(2L);
        occupiedTable.setTableNumber("2");
        occupiedTable.setStatus(TableStatus.OCCUPIED);
        occupiedTable.setCreatedAt(LocalDateTime.now());

        openDining = new Dining();
        openDining.setId(1L);
        openDining.setTableId(1L);
        openDining.markOpen();
        openDining.setCreatedAt(LocalDateTime.now());

        closedDining = new Dining();
        closedDining.setId(2L);
        closedDining.setTableId(2L);
        closedDining.markClosed();
        closedDining.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void openDining_withAvailableTable_success() {
        OpenDiningRequest request = new OpenDiningRequest(1L);
        when(tableService.findActive(1L)).thenReturn(availableTable);
        when(diningRepository.existsByTableIdAndStatus(1L, DiningStatus.OPEN)).thenReturn(false);
        when(diningRepository.save(any(Dining.class))).thenReturn(openDining);

        DiningResponse response = diningService.open(request);

        assertNotNull(response);
        assertEquals(DiningStatus.OPEN, response.status());
        verify(tableService, times(1)).findActive(1L);
        verify(diningRepository, times(1)).save(any(Dining.class));
    }

    @Test
    void openDining_withOccupiedTable_throwsBadRequest() {
        OpenDiningRequest request = new OpenDiningRequest(2L);
        when(tableService.findActive(2L)).thenReturn(occupiedTable);

        assertThrows(BadRequestException.class, () -> diningService.open(request));
    }

    @Test
    void openDining_withExistingOpenDining_throwsBadRequest() {
        OpenDiningRequest request = new OpenDiningRequest(1L);
        when(tableService.findActive(1L)).thenReturn(availableTable);
        when(diningRepository.existsByTableIdAndStatus(1L, DiningStatus.OPEN)).thenReturn(true);

        assertThrows(BadRequestException.class, () -> diningService.open(request));
    }

    @Test
    void closeDining_withAllOrdersCompletedAndPaid_success() {
        when(diningRepository.findById(1L)).thenReturn(Optional.of(openDining));
        when(tableService.findActive(1L)).thenReturn(availableTable);
        when(orderApi.getOrdersByDiningId(1L)).thenReturn(List.of(
            createOrderApiResponse(1L, "COMPLETED", "PAID", 50000),
            createOrderApiResponse(2L, "COMPLETED", "PAID", 30000)
        ));
        when(diningRepository.save(any(Dining.class))).thenReturn(closedDining);

        DiningResponse response = diningService.close(1L);

        assertNotNull(response);
        assertEquals(DiningStatus.CLOSED, response.status());
    }

    @Test
    void closeDining_withIncompleteOrders_throwsBadRequest() {
        when(diningRepository.findById(1L)).thenReturn(Optional.of(openDining));
        when(tableService.findActive(1L)).thenReturn(availableTable);
        when(orderApi.getOrdersByDiningId(1L)).thenReturn(List.of(
            createOrderApiResponse(1L, "COMPLETED", "PAID", 50000),
            createOrderApiResponse(2L, "PREPARING", "UNAPAID", 30000)
        ));

        assertThrows(BadRequestException.class, () -> diningService.close(1L));
    }

    @Test
    void closeDining_withUnpaidOrders_throwsBadRequest() {
        when(diningRepository.findById(1L)).thenReturn(Optional.of(openDining));
        when(tableService.findActive(1L)).thenReturn(availableTable);
        when(orderApi.getOrdersByDiningId(1L)).thenReturn(List.of(
            createOrderApiResponse(1L, "COMPLETED", "PAID", 50000),
            createOrderApiResponse(2L, "COMPLETED", "UNAPAID", 30000)
        ));

        assertThrows(BadRequestException.class, () -> diningService.close(1L));
    }

    @Test
    void closeDining_alreadyClosed_throwsBadRequest() {
        when(diningRepository.findById(2L)).thenReturn(Optional.of(closedDining));

        assertThrows(BadRequestException.class, () -> diningService.close(2L));
    }

    @Test
    void addOrder_toOpenDining_success() {
        CreateDiningOrderRequest request = new CreateDiningOrderRequest(
            1L, "John Doe", "Notes", List.of(
                new OrderItemApiRequest(1L, 2, List.of())
            )
        );

        OrderApiResponse orderResponse = createOrderApiResponse(1L, "CREATED", "UNAPAID", 50000);

        when(diningRepository.findById(1L)).thenReturn(Optional.of(openDining));
        when(orderApi.createDineInOrder(eq(1L), any(OrderApiCreateRequest.class))).thenReturn(orderResponse);
        when(tableService.findActive(1L)).thenReturn(availableTable);
        when(orderApi.getOrdersByDiningId(1L)).thenReturn(List.of(orderResponse));

        DiningResponse response = diningService.addOrder(1L, request);

        assertNotNull(response);
        assertEquals(1, response.orders().size());
    }

    @Test
    void addOrder_toClosedDining_throwsBadRequest() {
        CreateDiningOrderRequest request = new CreateDiningOrderRequest(
            1L, "John Doe", null, List.of(
                new OrderItemApiRequest(1L, 1, List.of())
            )
        );

        when(diningRepository.findById(2L)).thenReturn(Optional.of(closedDining));

        assertThrows(BadRequestException.class, () -> diningService.addOrder(2L, request));
    }

    @Test
    void getDining_withOrders_returnsCorrectTotal() {
        when(diningRepository.findById(1L)).thenReturn(Optional.of(openDining));
        when(tableService.findActive(1L)).thenReturn(availableTable);
        when(orderApi.getOrdersByDiningId(1L)).thenReturn(List.of(
            createOrderApiResponse(1L, "COMPLETED", "PAID", 50000),
            createOrderApiResponse(2L, "CANCELLED", "UNAPAID", 30000),
            createOrderApiResponse(3L, "READY", "UNPAID", 20000)
        ));

        DiningResponse response = diningService.getById(1L);

        assertNotNull(response);
        assertEquals(70000, response.totalPrice());
        assertEquals(3, response.orders().size());
    }

    @Test
    void getDining_notFound_throwsNotFound() {
        when(diningRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> diningService.getById(999L));
    }

    private OrderApiResponse createOrderApiResponse(Long id, String status, String paidStatus, Integer totalPrice) {
        return new OrderApiResponse(
            id,
            OrderTypeApiResponse.DINE_IN,
            status,
            paidStatus,
            "ORD-20260828-TEST" + id,
            1L,
            "Customer",
            1L,
            totalPrice,
            LocalDateTime.now()
        );
    }
}
