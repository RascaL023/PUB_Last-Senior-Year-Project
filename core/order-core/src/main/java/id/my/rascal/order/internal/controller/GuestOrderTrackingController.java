package id.my.rascal.order.internal.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import id.my.rascal.common.ApiResponse;
import id.my.rascal.common.template.SuccessTemplate;
import id.my.rascal.order.internal.model.response.GuestOrderTrackingResponse;
import id.my.rascal.order.internal.service.OrderQueryService;

@RestController
@RequestMapping("/api/v1/guest/orders")
public class GuestOrderTrackingController {

    private final OrderQueryService orderQueryService;

    public GuestOrderTrackingController(OrderQueryService orderQueryService) {
        this.orderQueryService = orderQueryService;
    }

    @GetMapping("/{trackToken}")
    public ResponseEntity<SuccessTemplate<GuestOrderTrackingResponse>> getByTrackToken(
        @PathVariable("trackToken") String trackToken
    ) {
        return ApiResponse.success(
            HttpStatus.OK,
            "Order successfully retrieved",
            orderQueryService.findActiveByTrackToken(trackToken)
        );
    }

}
