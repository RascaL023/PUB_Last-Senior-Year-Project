package id.my.rascal.dining.internal.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import id.my.rascal.common.ApiResponse;
import id.my.rascal.common.template.SuccessTemplate;
import id.my.rascal.dining.internal.model.request.GuestOrderRequest;
import id.my.rascal.dining.internal.model.response.GuestDiningResponse;
import id.my.rascal.dining.internal.service.GuestDiningService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/guest/dinings")
public class GuestDiningController {

    private final GuestDiningService guestDiningService;

    public GuestDiningController(GuestDiningService guestDiningService) {
        this.guestDiningService = guestDiningService;
    }

    @GetMapping("/{guestToken}")
    public ResponseEntity<SuccessTemplate<GuestDiningResponse>> getByToken(
        @PathVariable("guestToken") String guestToken
    ) {
        return ApiResponse.success(
            HttpStatus.OK,
            "Dining session successfully retrieved",
            guestDiningService.getByToken(guestToken)
        );
    }

    @GetMapping("/by-code/{guestCode}")
    public ResponseEntity<SuccessTemplate<GuestDiningResponse>> getByCode(
        @PathVariable("guestCode") String guestCode
    ) {
        return ApiResponse.success(
            HttpStatus.OK,
            "Dining session successfully retrieved",
            guestDiningService.getByCode(guestCode)
        );
    }

    @PostMapping("/{guestToken}/orders")
    public ResponseEntity<SuccessTemplate<GuestDiningResponse>> addOrder(
        @PathVariable("guestToken") String guestToken,
        @Valid @RequestBody GuestOrderRequest request
    ) {
        return ApiResponse.success(
            HttpStatus.CREATED,
            "Order successfully added to dining",
            guestDiningService.addOrder(guestToken, request)
        );
    }

}
