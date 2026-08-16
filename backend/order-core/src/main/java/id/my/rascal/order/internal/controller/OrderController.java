package id.my.rascal.order.internal.controller;

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
import id.my.rascal.order.internal.model.enums.OrderStatus;
import id.my.rascal.order.internal.model.enums.PaymentStatus;
import id.my.rascal.order.internal.model.request.OrderPatchRequest;
import id.my.rascal.order.internal.model.request.OrderPutRequest;
import id.my.rascal.order.internal.model.request.OrderRequest;
import id.my.rascal.order.internal.model.response.OrderResponse;
import id.my.rascal.order.internal.service.OrderService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final String DEFAULT_GET_SUCCESS_MESSAGE = "Order successfully retrieved";
    private final String DEFAULT_CREATE_SUCCESS_MESSAGE = "Order successfully created";
    private final String DEFAULT_UPDATE_SUCCESS_MESSAGE = "Order successfully updated";

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<SuccessTemplate<OrderResponse>> create(@Valid @RequestBody OrderRequest request) {
        return ApiResponse.success(
            HttpStatus.CREATED,
            DEFAULT_CREATE_SUCCESS_MESSAGE,
            orderService.create(request)
        );
    }

    @GetMapping
    public ResponseEntity<SuccessPagedTemplate<List<OrderResponse>>> getAll(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) OrderStatus status,
        @RequestParam(required = false) PaymentStatus paymentStatus,
        @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<OrderResponse> page = orderService.search(keyword, status, paymentStatus, pageable);

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
    public ResponseEntity<SuccessTemplate<OrderResponse>> getById(@PathVariable("id") Long id) {
        return ApiResponse.success(
            HttpStatus.OK,
            DEFAULT_GET_SUCCESS_MESSAGE,
            orderService.getById(id)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<SuccessTemplate<OrderResponse>> update(
        @PathVariable("id") Long id,
        @Valid @RequestBody OrderPutRequest request
    ) {
        return ApiResponse.success(
            HttpStatus.OK,
            DEFAULT_UPDATE_SUCCESS_MESSAGE,
            orderService.update(id, request)
        );
    }

    @PatchMapping("/{id}")
    public ResponseEntity<SuccessTemplate<OrderResponse>> patch(
        @PathVariable("id") Long id,
        @RequestBody OrderPatchRequest request
    ) {
        if (request.isEmptyPatch())
            throw new BadRequestException("PATCH can't be empty");

        return ApiResponse.success(
            HttpStatus.OK,
            DEFAULT_UPDATE_SUCCESS_MESSAGE,
            orderService.patch(id, request)
        );
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<SuccessTemplate<OrderResponse>> updateStatus(
        @PathVariable("id") Long id,
        @RequestParam OrderStatus status
    ) {
        return ApiResponse.success(
            HttpStatus.OK,
            DEFAULT_UPDATE_SUCCESS_MESSAGE,
            orderService.updateStatus(id, status)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        orderService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
