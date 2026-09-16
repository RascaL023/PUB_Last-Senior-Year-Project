package id.my.rascal.invoice.internal.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import id.my.rascal.common.ApiResponse;
import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.common.template.SuccessPagedTemplate;
import id.my.rascal.common.template.SuccessTemplate;
import id.my.rascal.invoice.internal.entity.InvoiceStatus;
import id.my.rascal.invoice.internal.model.request.CreateInvoiceRequest;
import id.my.rascal.invoice.internal.model.response.InvoiceResponse;
import id.my.rascal.invoice.internal.service.InvoiceQueryService;
import id.my.rascal.invoice.internal.service.InvoiceService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoiceQueryService invoiceQueryService;
    private final String DEFAULT_GET_SUCCESS_MESSAGE = "Invoice successfully retrieved";
    private final String DEFAULT_CREATE_SUCCESS_MESSAGE = "Invoice successfully created";
    private final String DEFAULT_UPDATE_SUCCESS_MESSAGE = "Invoice successfully updated";

    public InvoiceController(InvoiceService invoiceService, InvoiceQueryService invoiceQueryService) {
        this.invoiceService = invoiceService;
        this.invoiceQueryService = invoiceQueryService;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('invoice.create', 'invoice.*')")
    public ResponseEntity<SuccessTemplate<InvoiceResponse>> create(@Valid @RequestBody CreateInvoiceRequest request) {
        return ApiResponse.success(
            HttpStatus.CREATED,
            DEFAULT_CREATE_SUCCESS_MESSAGE,
            invoiceService.create(request)
        );
    }

    @PostMapping("/{id}/void")
    @PreAuthorize("hasAnyAuthority('invoice.update', 'invoice.*')")
    public ResponseEntity<SuccessTemplate<InvoiceResponse>> voidInvoice(@PathVariable Long id) {
        validateId(id);
        return ApiResponse.success(
            HttpStatus.OK,
            "Invoice successfully voided",
            invoiceService.voidInvoice(id)
        );
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('invoice.read', 'invoice.*')")
    public ResponseEntity<SuccessPagedTemplate<List<InvoiceResponse>>> getAll(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) Long diningId,
        @RequestParam(required = false) Long orderId,
        @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<InvoiceResponse> page = invoiceQueryService.searchActive(
            keyword,
            InvoiceStatus.fromString(status),
            diningId,
            orderId,
            pageable
        );

        return ApiResponse.paged(
            HttpStatus.OK,
            DEFAULT_GET_SUCCESS_MESSAGE,
            page.getContent(),
            page.getNumber() + 1,
            page.getSize(),
            page.getTotalElements(),
            page.hasNext(),
            page.hasPrevious()
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('invoice.read', 'invoice.*')")
    public ResponseEntity<SuccessTemplate<InvoiceResponse>> getById(@PathVariable("id") Long id) {
        return ApiResponse.success(
            HttpStatus.OK,
            DEFAULT_GET_SUCCESS_MESSAGE,
            invoiceQueryService.findActiveInvoiceById(id)
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('invoice.delete', 'invoice.*')")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        invoiceService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private void validateId(Long id) {
        if (id == null || id < 1) throw new BadRequestException("Invalid invoice ID");
    }

}
