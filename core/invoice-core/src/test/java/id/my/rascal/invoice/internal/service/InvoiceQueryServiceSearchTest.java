package id.my.rascal.invoice.internal.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import id.my.rascal.invoice.internal.entity.InvoiceStatus;
import id.my.rascal.invoice.internal.repository.InvoiceRepository;

class InvoiceQueryServiceSearchTest {

    @Test
    void searchActive_forwardsDiningAndOrderFilters() {
        InvoiceRepository invoiceRepository = mock(InvoiceRepository.class);
        when(invoiceRepository.searchActive(any(), any(), any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of()));
        InvoiceQueryService queryService = new InvoiceQueryService(invoiceRepository);

        queryService.searchActive(null, InvoiceStatus.OPEN, 20L, 101L, Pageable.ofSize(10));

        verify(invoiceRepository).searchActive(
            eq(""), eq(InvoiceStatus.OPEN), eq(20L), eq(101L), any(Pageable.class)
        );
    }

}
