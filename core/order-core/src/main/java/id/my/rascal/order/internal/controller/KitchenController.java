package id.my.rascal.order.internal.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import id.my.rascal.common.ApiResponse;
import id.my.rascal.common.template.SuccessTemplate;
import id.my.rascal.order.internal.model.enums.OrderStatus;
import id.my.rascal.order.internal.model.response.KitchenTicketResponse;
import id.my.rascal.order.internal.service.KitchenService;

@RestController
@RequestMapping("/api/v1/kitchen")
public class KitchenController {

    private final KitchenService kitchenService;

    public KitchenController(KitchenService kitchenService) {
        this.kitchenService = kitchenService;
    }

    @GetMapping("/orders")
    @PreAuthorize("hasAnyAuthority('kitchen.read', 'kitchen.*')")
    public ResponseEntity<SuccessTemplate<List<KitchenTicketResponse>>> getQueue(
        @RequestParam(required = false) List<String> status,
        @RequestParam(required = false, defaultValue = "" + KitchenService.DEFAULT_SIZE) int size
    ) {
        return ApiResponse.success(
            HttpStatus.OK,
            "Kitchen queue successfully retrieved",
            kitchenService.getQueue(OrderStatus.fromStrings(status), size)
        );
    }

}
