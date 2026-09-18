package id.my.rascal.dining.internal.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import id.my.rascal.common.ApiResponse;
import id.my.rascal.common.template.SuccessTemplate;
import id.my.rascal.dining.internal.entity.DiningStatus;
import id.my.rascal.dining.internal.model.request.GuestOrderRequest;
import id.my.rascal.dining.internal.model.response.GuestDiningResponse;
import id.my.rascal.dining.internal.model.response.MyDiningResponse;
import id.my.rascal.dining.internal.service.GuestDiningService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/my/dinings")
@PreAuthorize("isAuthenticated()")
public class MyDiningController {

    private final GuestDiningService guestDiningService;

    public MyDiningController(GuestDiningService guestDiningService) {
        this.guestDiningService = guestDiningService;
    }

    @GetMapping
    public ResponseEntity<SuccessTemplate<List<MyDiningResponse>>> getMySessions(
        Authentication authentication,
        @RequestParam(required = false) String status
    ) {
        return ApiResponse.success(
            HttpStatus.OK,
            "My dining sessions successfully retrieved",
            guestDiningService.findMySessions(
                Long.valueOf(authentication.getName()),
                DiningStatus.fromString(status)
            )
        );
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

    @PostMapping("/{guestToken}/orders")
    public ResponseEntity<SuccessTemplate<GuestDiningResponse>> addOrder(
        Authentication authentication,
        @PathVariable("guestToken") String guestToken,
        @Valid @RequestBody GuestOrderRequest request
    ) {
        Long userAuthId = Long.valueOf(authentication.getName());
        return ApiResponse.success(
            HttpStatus.CREATED,
            "Order successfully added to dining",
            guestDiningService.addOrderAsMember(guestToken, userAuthId, request)
        );
    }

}
