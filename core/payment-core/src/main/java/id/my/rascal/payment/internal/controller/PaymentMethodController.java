package id.my.rascal.payment.internal.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import id.my.rascal.payment.internal.model.request.PaymentMethodPatchRequest;
import id.my.rascal.payment.internal.model.request.PaymentMethodPutRequest;
import id.my.rascal.payment.internal.model.request.PaymentMethodRequest;
import id.my.rascal.payment.internal.model.response.PaymentMethodResponse;
import id.my.rascal.payment.internal.service.PaymentMethodService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/payment-methods")
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;
    private final String DEFAULT_GET_SUCCESS_MESSAGE = "Payment method successfully retrieved";
    private final String DEFAULT_CREATE_SUCCESS_MESSAGE = "Payment method successfully created";
    private final String DEFAULT_UPDATE_SUCCESS_MESSAGE = "Payment method successfully updated";

    public PaymentMethodController(PaymentMethodService paymentMethodService) {
        this.paymentMethodService = paymentMethodService;
    }

    @PostMapping
    public ResponseEntity<SuccessTemplate<PaymentMethodResponse>> create(
        @Valid @RequestBody PaymentMethodRequest request
    ) {
        return ApiResponse.success(
            HttpStatus.CREATED,
            DEFAULT_CREATE_SUCCESS_MESSAGE,
            paymentMethodService.create(request)
        );
    }

    @GetMapping
    public ResponseEntity<SuccessPagedTemplate<List<PaymentMethodResponse>>> getAll(
        @RequestParam(required = false) String keyword,
        @PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Page<PaymentMethodResponse> page = paymentMethodService.search(keyword, pageable);

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
    public ResponseEntity<SuccessTemplate<PaymentMethodResponse>> getById(@PathVariable("id") Long id) {
        return ApiResponse.success(
            HttpStatus.OK,
            DEFAULT_GET_SUCCESS_MESSAGE,
            paymentMethodService.getById(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<SuccessTemplate<PaymentMethodResponse>> update(
        @PathVariable("id") Long id,
        @Valid @RequestBody PaymentMethodPutRequest request
    ) {
        return ApiResponse.success(
            HttpStatus.OK,
            DEFAULT_UPDATE_SUCCESS_MESSAGE,
            paymentMethodService.update(id, request)
        );
    }

    @PatchMapping("/{id}")
    public ResponseEntity<SuccessTemplate<PaymentMethodResponse>> patch(
        @PathVariable("id") Long id,
        @RequestBody PaymentMethodPatchRequest request
    ) {
        if (request.isEmptyPatch())
            throw new BadRequestException("PATCH can't be empty");

        return ApiResponse.success(
            HttpStatus.OK,
            DEFAULT_UPDATE_SUCCESS_MESSAGE,
            paymentMethodService.patch(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        paymentMethodService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
