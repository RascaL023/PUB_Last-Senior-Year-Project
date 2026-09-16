package id.my.rascal.customer.internal.controller;

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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import id.my.rascal.common.ApiResponse;
import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.common.template.SuccessPagedTemplate;
import id.my.rascal.common.template.SuccessTemplate;
import id.my.rascal.customer.internal.model.request.CustomerPatchRequest;
import id.my.rascal.customer.internal.model.request.CustomerPutRequest;
import id.my.rascal.customer.internal.model.request.CustomerRequest;
import id.my.rascal.customer.internal.model.response.CustomerResponse;
import id.my.rascal.customer.internal.service.CustomerService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    // @PreAuthorize("hasAnyAuthority('customer.create', 'customer.*')")
    public ResponseEntity<SuccessTemplate<CustomerResponse>> create(
        @Valid @RequestBody CustomerRequest request
    ) {
        return ApiResponse.success(
            HttpStatus.CREATED,
            "Customer successfully created",
            customerService.create(request)
        );
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('customer.read', 'customer.*')")
    public ResponseEntity<SuccessPagedTemplate<List<CustomerResponse>>> getAll(
        @RequestParam(required = false) String keyword,
        @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<CustomerResponse> page = customerService.search(keyword, pageable);

        return ApiResponse.paged(
            HttpStatus.OK,
            "Customers successfully retrieved",
            page.getContent(),
            page.getNumber() + 1,
            page.getSize(),
            page.getTotalElements(),
            page.hasNext(),
            page.hasPrevious()
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('customer.read', 'customer.*')")
    public ResponseEntity<SuccessTemplate<CustomerResponse>> getById(@PathVariable("id") Long id) {
        return ApiResponse.success(
            HttpStatus.OK,
            "Customer successfully retrieved",
            customerService.getById(id)
        );
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('customer.update', 'customer.*')")
    public ResponseEntity<SuccessTemplate<CustomerResponse>> update(
        @PathVariable("id") Long id,
        @Valid @RequestBody CustomerPutRequest request
    ) {
        return ApiResponse.success(
            HttpStatus.OK,
            "Customer successfully updated",
            customerService.update(id, request)
        );
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('customer.update', 'customer.*')")
    public ResponseEntity<SuccessTemplate<CustomerResponse>> patch(
        @PathVariable("id") Long id,
        @RequestBody CustomerPatchRequest request
    ) {
        if (request.isEmptyPatch())
            throw new BadRequestException("PATCH can't be empty");

        return ApiResponse.success(
            HttpStatus.OK,
            "Customer successfully updated",
            customerService.patch(id, request)
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('customer.delete', 'customer.*')")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        customerService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
