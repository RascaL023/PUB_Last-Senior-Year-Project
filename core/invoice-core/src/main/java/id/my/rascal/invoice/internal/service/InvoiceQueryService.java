package id.my.rascal.invoice.internal.service;

import id.my.rascal.common.exception.NotFoundException;
import id.my.rascal.common.util.StringUtil;
import id.my.rascal.invoice.api.InvoiceApiResponse;
import id.my.rascal.invoice.internal.entity.Invoice;
import id.my.rascal.invoice.internal.entity.InvoiceStatus;
import id.my.rascal.invoice.internal.model.mapper.InvoiceMapper;
import id.my.rascal.invoice.internal.model.response.InvoiceResponse;
import id.my.rascal.invoice.internal.repository.InvoiceRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class InvoiceQueryService {

    private final InvoiceRepository invoiceRepository;

    public InvoiceQueryService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    @Transactional(readOnly = true)
    public InvoiceResponse findActiveInvoiceById(Long id) {
        return InvoiceMapper.toResponse(findActiveInvoice(id));
    }

    @Transactional(readOnly = true)
    public List<InvoiceApiResponse> findActiveInvoicesByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();

        Map<Long, InvoiceApiResponse> responseMap = invoiceRepository.findAllById(ids).stream()
            .filter(i -> i.getDeletedAt() == null)
            .map(InvoiceMapper::toApiResponse)
            .collect(Collectors.toMap(InvoiceApiResponse::id, Function.identity()));

        return ids.stream()
            .map(responseMap::get)
            .filter(java.util.Objects::nonNull)
            .toList();
    }

    @Transactional(readOnly = true)
    public InvoiceApiResponse findActiveInvoiceByDiningId(Long diningId) {
        return invoiceRepository.findActiveByDiningId(diningId)
            .map(InvoiceMapper::toApiResponse)
            .orElse(null);
    }

    @Transactional(readOnly = true)
    public boolean hasAppliedPayment(Long orderId) {
        return invoiceRepository.findActiveByItemsOrderId(orderId).stream()
            .anyMatch(inv -> inv.getPaidAmount() != null && inv.getPaidAmount() > 0);
    }

    @Transactional(readOnly = true)
    public String findStatusByOrderId(Long orderId) {
        return invoiceRepository.findActiveByItemsOrderId(orderId).stream()
            .findFirst()
            .map(inv -> inv.getStatus().name())
            .orElse(null);
    }

    @Transactional(readOnly = true)
    public Page<InvoiceResponse> searchActive(
        String keyword,
        InvoiceStatus status,
        Long diningId,
        Long orderId,
        Pageable pageable
    ) {
        return invoiceRepository
            .searchActive(StringUtil.normalizeSearch(keyword), status, diningId, orderId, pageable)
            .map(InvoiceMapper::toResponse);
    }

    private Invoice findActiveInvoice(Long id) {
        return invoiceRepository.findActiveById(id)
            .orElseThrow(() -> new NotFoundException("Invoice not found with id: " + id));
    }

}
