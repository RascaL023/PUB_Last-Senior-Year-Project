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
import id.my.rascal.payment.internal.model.enums.PaymentProvider;
import id.my.rascal.payment.internal.model.enums.PaymentStatus;
import id.my.rascal.payment.internal.model.enums.PaymentTargetType;
import id.my.rascal.payment.internal.model.request.PaymentPatchRequest;
import id.my.rascal.payment.internal.model.request.PaymentPutRequest;
import id.my.rascal.payment.internal.model.request.PaymentRequest;
import id.my.rascal.payment.internal.model.response.PaymentResponse;
import id.my.rascal.payment.internal.service.PaymentService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;
    private final String DEFAULT_GET_SUCCESS_MESSAGE = "Payment successfully retrieved";
    private final String DEFAULT_CREATE_SUCCESS_MESSAGE = "Payment successfully created";
    private final String DEFAULT_UPDATE_SUCCESS_MESSAGE = "Payment successfully updated";

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<SuccessTemplate<PaymentResponse>> create(@Valid @RequestBody PaymentRequest request) {
        return ApiResponse.success(
            HttpStatus.CREATED,
            DEFAULT_CREATE_SUCCESS_MESSAGE,
            paymentService.create(request)
        );
    }

    // @PostMapping("/{id}/pay")
    // public ResponseEntity<SuccessTemplate<PaymentResponse>> pay(@PathVariable Long id) {
    //     validateId(id);
    //     return ApiResponse.success(HttpStatus.OK, "Payment successfully marked as paid", paymentService.markPaid(id));
    // }

    @PostMapping("/{id}/expire")
    public ResponseEntity<SuccessTemplate<PaymentResponse>> expire(@PathVariable Long id) {
        validateId(id);
        return ApiResponse.success(HttpStatus.OK, "Payment successfully marked as expired", paymentService.markExpired(id));
    }

    @PostMapping("/{id}/fail")
    public ResponseEntity<SuccessTemplate<PaymentResponse>> fail(@PathVariable Long id) {
        validateId(id);
        return ApiResponse.success(HttpStatus.OK, "Payment successfully marked as failed", paymentService.markFailed(id));
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<SuccessTemplate<PaymentResponse>> refund(@PathVariable Long id) {
        validateId(id);
        return ApiResponse.success(HttpStatus.OK, "Payment successfully marked as refunded", paymentService.markRefunded(id));
    }

    // TODO: resolve payment provider enum
    @GetMapping
    public ResponseEntity<SuccessPagedTemplate<List<PaymentResponse>>> getAll(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String targetType,
        @RequestParam(required = false) Long targetId,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String paymentProvider,
        @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<PaymentResponse> page = paymentService.search(
            keyword, 
            PaymentTargetType.fromString(targetType), 
            targetId, 
            PaymentStatus.fromString(status), 
            PaymentProvider.valueOf(paymentProvider.toUpperCase()), 
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
    public ResponseEntity<SuccessTemplate<PaymentResponse>> getById(@PathVariable("id") Long id) {
        return ApiResponse.success(
            HttpStatus.OK,
            DEFAULT_GET_SUCCESS_MESSAGE,
            paymentService.getById(id)
        );
    }

    // @PutMapping("/{id}")
    // public ResponseEntity<SuccessTemplate<PaymentResponse>> update(
    //     @PathVariable("id") Long id,
    //     @Valid @RequestBody PaymentPutRequest request
    // ) {
    //     return ApiResponse.success(
    //         HttpStatus.OK,
    //         DEFAULT_UPDATE_SUCCESS_MESSAGE,
    //         paymentService.update(id, request)
    //     );
    // }

    // @PatchMapping("/{id}")
    // public ResponseEntity<SuccessTemplate<PaymentResponse>> patch(
    //     @PathVariable("id") Long id,
    //     @RequestBody PaymentPatchRequest request
    // ) {
    //     if (request.isEmptyPatch())
    //         throw new BadRequestException("PATCH can't be empty");
    //
    //     return ApiResponse.success(
    //         HttpStatus.OK,
    //         DEFAULT_UPDATE_SUCCESS_MESSAGE,
    //         paymentService.patch(id, request)
    //     );
    // }

    // @DeleteMapping("/{id}")
    // public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
    //     paymentService.delete(id);
    //     return ResponseEntity.noContent().build();
    // }

    private void validateId(Long id) {
        if (id < 0) throw new BadRequestException("Invalid payment ID");
    }
}
