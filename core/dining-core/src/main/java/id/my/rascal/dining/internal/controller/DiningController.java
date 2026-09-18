package id.my.rascal.dining.internal.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import id.my.rascal.common.ApiResponse;
import id.my.rascal.common.template.SuccessPagedTemplate;
import id.my.rascal.common.template.SuccessTemplate;
import id.my.rascal.dining.internal.entity.DiningStatus;
import id.my.rascal.dining.internal.model.request.CreateDiningOrderRequest;
import id.my.rascal.dining.internal.model.request.OpenDiningRequest;
import id.my.rascal.dining.internal.model.response.DiningResponse;
import id.my.rascal.dining.internal.service.DiningService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/dinings")
public class DiningController {

    private final DiningService diningService;

    public DiningController(DiningService diningService) {
        this.diningService = diningService;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('dining.create', 'dining.*')")
    public ResponseEntity<SuccessTemplate<DiningResponse>> open(
        @Valid @RequestBody OpenDiningRequest request
    ) {
        return ApiResponse.success(
            HttpStatus.CREATED,
            "Dining session successfully opened",
            diningService.open(request)
        );
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('dining.read', 'dining.*')")
    public ResponseEntity<SuccessPagedTemplate<List<DiningResponse>>> getAll(
        @RequestParam(required = false) String status,
        @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<DiningResponse> page = diningService.search(DiningStatus.fromString(status), pageable);

        return ApiResponse.paged(
            HttpStatus.OK,
            "Dinings successfully retrieved",
            page.getContent(),
            page.getNumber() + 1,
            page.getSize(),
            page.getTotalElements(),
            page.hasNext(),
            page.hasPrevious()
        );
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('dining.read', 'dining.*')")
    public ResponseEntity<SuccessTemplate<DiningResponse>> getById(@PathVariable("id") Long id) {
        return ApiResponse.success(
            HttpStatus.OK,
            "Dining successfully retrieved",
            diningService.getById(id)
        );
    }

    @PostMapping("/{id}/orders")
    @PreAuthorize("hasAnyAuthority('dining.update', 'dining.*')")
    public ResponseEntity<SuccessTemplate<DiningResponse>> addOrder(
        @PathVariable("id") Long id,
        @Valid @RequestBody CreateDiningOrderRequest request
    ) {
        return ApiResponse.success(
            HttpStatus.CREATED,
            "Order successfully added to dining",
            diningService.addOrder(id, request)
        );
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAnyAuthority('dining.update', 'dining.*')")
    public ResponseEntity<SuccessTemplate<DiningResponse>> close(@PathVariable("id") Long id) {
        return ApiResponse.success(
            HttpStatus.OK,
            "Dining session successfully closed",
            diningService.close(id)
        );
    }

}
