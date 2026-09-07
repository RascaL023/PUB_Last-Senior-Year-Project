package id.my.rascal.invoice.internal.adapter;

import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Service;

import id.my.rascal.invoice.api.InvoiceApi;
import id.my.rascal.invoice.api.InvoiceApiResponse;
import id.my.rascal.invoice.internal.model.mapper.InvoiceMapper;
import id.my.rascal.invoice.internal.service.InvoiceQueryService;

@Service
public class InvoiceApiImpl implements InvoiceApi {

    private final InvoiceQueryService invoiceQueryService;

    public InvoiceApiImpl(InvoiceQueryService invoiceQueryService) {
        this.invoiceQueryService = invoiceQueryService;
    }

    @Override
    public InvoiceApiResponse getInvoice(Long id) {
        return InvoiceMapper.toApiResponse(invoiceQueryService.findActiveInvoiceById(id));
    }

    @Override
    public List<InvoiceApiResponse> getInvoices(Collection<Long> ids) {
        return invoiceQueryService.findActiveInvoicesByIds(ids);
    }

}
