package id.my.rascal.order.internal.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import id.my.rascal.common.exception.ForbiddenException;
import id.my.rascal.customer.api.CustomerApi;
import id.my.rascal.customer.api.CustomerApiResponse;
import id.my.rascal.invoice.api.InvoiceApi;
import id.my.rascal.order.internal.repository.OrderItemRepository;
import id.my.rascal.order.internal.repository.OrderRepository;

class MyOrderQueryTest {

    private static final Long USER_AUTH_ID = 55L;
    private static final Long CUSTOMER_ID = 77L;

    private OrderRepository orderRepository;
    private CustomerApi customerApi;
    private OrderQueryService orderQueryService;

    @BeforeEach
    void setUp() {
        orderRepository = mock(OrderRepository.class);
        customerApi = mock(CustomerApi.class);
        orderQueryService = new OrderQueryService(
            orderRepository,
            mock(OrderItemRepository.class),
            customerApi,
            mock(InvoiceApi.class)
        );
    }

    @Test
    void findMyOrders_withoutMemberProfile_isForbidden() {
        when(customerApi.getByUserAuthId(USER_AUTH_ID)).thenReturn(Optional.empty());

        assertThrows(
            ForbiddenException.class,
            () -> orderQueryService.findMyOrders(USER_AUTH_ID, Pageable.ofSize(10))
        );
    }

    @Test
    void findMyOrders_withMemberProfile_scopesQueryToCustomerId() {
        when(customerApi.getByUserAuthId(USER_AUTH_ID)).thenReturn(Optional.of(member()));
        when(orderRepository.searchActiveByCustomerId(any(), any())).thenReturn(new PageImpl<>(List.of()));

        orderQueryService.findMyOrders(USER_AUTH_ID, Pageable.ofSize(10));

        verify(orderRepository).searchActiveByCustomerId(CUSTOMER_ID, Pageable.ofSize(10));
    }

    private CustomerApiResponse member() {
        return new CustomerApiResponse(CUSTOMER_ID, USER_AUTH_ID, "Budi", "budi@rascal.id", "0812");
    }

}
