package id.my.rascal.dining.internal.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.dining.internal.entity.DiningStatus;
import id.my.rascal.dining.internal.entity.TableStatus;
import id.my.rascal.dining.internal.repository.DiningRepository;
import id.my.rascal.dining.internal.repository.DiningTableRepository;

class DiningFloorFilterTest {

    private DiningRepository diningRepository;
    private DiningTableRepository diningTableRepository;
    private DiningService diningService;
    private TableService tableService;

    @BeforeEach
    void setUp() {
        diningRepository = mock(DiningRepository.class);
        diningTableRepository = mock(DiningTableRepository.class);
        when(diningRepository.findAllPaged(any(), any())).thenReturn(Page.empty());
        when(diningTableRepository.searchActive(any(), any(), any())).thenReturn(Page.empty());

        tableService = new TableService(diningTableRepository);
        diningService = new DiningService(
            diningRepository,
            mock(id.my.rascal.dining.internal.repository.DiningOrderRepository.class),
            diningTableRepository,
            tableService,
            mock(id.my.rascal.order.api.OrderApi.class),
            mock(id.my.rascal.invoice.api.InvoiceApi.class),
            mock(DiningEventPublisherService.class),
            new GuestTokenGenerator(),
            mock(id.my.rascal.customer.api.CustomerApi.class)
        );
    }

    @Test
    void diningSearch_forwardsOpenStatus() {
        diningService.search(DiningStatus.OPEN, Pageable.ofSize(10));

        verify(diningRepository).findAllPaged(DiningStatus.OPEN, Pageable.ofSize(10));
    }

    @Test
    void diningSearch_withoutStatus_listsEverySession() {
        diningService.search(null, Pageable.ofSize(10));

        verify(diningRepository).findAllPaged(null, Pageable.ofSize(10));
    }

    @Test
    void tableSearch_forwardsAvailableStatus() {
        tableService.search(null, TableStatus.AVAILABLE, Pageable.ofSize(10));

        verify(diningTableRepository).searchActive("", TableStatus.AVAILABLE, Pageable.ofSize(10));
    }

    @Test
    void diningStatus_parsesAndRejectsUnknownValue() {
        assertEquals(DiningStatus.OPEN, DiningStatus.fromString("open"));
        assertNull(DiningStatus.fromString("  "));
        assertThrows(BadRequestException.class, () -> DiningStatus.fromString("PAID"));
    }

    @Test
    void tableStatus_parsesAndRejectsUnknownValue() {
        assertEquals(TableStatus.OCCUPIED, TableStatus.fromString("occupied"));
        assertNull(TableStatus.fromString(null));
        assertThrows(BadRequestException.class, () -> TableStatus.fromString("DIRTY"));
    }

}
