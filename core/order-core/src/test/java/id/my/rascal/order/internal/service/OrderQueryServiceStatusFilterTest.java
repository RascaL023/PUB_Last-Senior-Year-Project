package id.my.rascal.order.internal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.order.internal.model.enums.OrderStatus;
import id.my.rascal.order.internal.repository.OrderRepository;

class OrderQueryServiceStatusFilterTest {

    private OrderRepository orderRepository;
    private OrderQueryService orderQueryService;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        orderQueryService = new OrderQueryService(orderRepository);
        when(orderRepository.searchActive(any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of()));
    }

    @Test
    void searchActive_withoutStatus_filtersByEveryStatus() {
        orderQueryService.searchActive(null, null, Pageable.ofSize(10));

        assertEquals(List.of(OrderStatus.values()), capturedStatuses());
    }

    @Test
    void searchActive_withEmptyStatus_filtersByEveryStatus() {
        orderQueryService.searchActive(null, List.of(), Pageable.ofSize(10));

        assertEquals(List.of(OrderStatus.values()), capturedStatuses());
    }

    @Test
    void searchActive_withStatuses_forwardsExactlyThose() {
        orderQueryService.searchActive(
            null,
            List.of(OrderStatus.CONFIRMED, OrderStatus.PREPARING),
            Pageable.ofSize(10)
        );

        assertEquals(List.of(OrderStatus.CONFIRMED, OrderStatus.PREPARING), capturedStatuses());
    }

    @Test
    void fromStrings_mapsAliasesAndDropsDuplicates() {
        assertEquals(
            List.of(OrderStatus.CONFIRMED, OrderStatus.PREPARING, OrderStatus.CANCELLED),
            OrderStatus.fromStrings(List.of("confirmed", "prepare", "CANCELLED", "cancel"))
        );
    }

    @Test
    void fromStrings_blankInput_meansNoFilter() {
        assertNull(OrderStatus.fromStrings(null));
        assertNull(OrderStatus.fromStrings(List.of()));
        assertNull(OrderStatus.fromStrings(List.of("", "   ")));
    }

    @Test
    void fromStrings_unknownStatus_rejected() {
        assertThrows(BadRequestException.class, () -> OrderStatus.fromStrings(List.of("SERVED")));
    }

    @SuppressWarnings("unchecked")
    private List<OrderStatus> capturedStatuses() {
        ArgumentCaptor<Collection<OrderStatus>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(orderRepository).searchActive(any(), captor.capture(), any());
        return List.copyOf(captor.getValue());
    }

}
