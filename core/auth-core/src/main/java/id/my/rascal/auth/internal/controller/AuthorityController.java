package id.my.rascal.auth.internal.controller;

import id.my.rascal.auth.internal.model.response.AuthorityResponse;
import id.my.rascal.auth.internal.service.AuthorityService;
import id.my.rascal.common.ApiResponse;
import id.my.rascal.common.exception.BadRequestException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auths/authorities")
public class AuthorityController {

    private final AuthorityService authorityService;

    public AuthorityController(AuthorityService authorityService) {
        this.authorityService = authorityService;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('authority.create', 'authority.*')")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        AuthorityResponse data = authorityService.getById(id);
        return ApiResponse.success(HttpStatus.OK, data);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('authority.read', 'authority.*')")
    public ResponseEntity<?> getAll(
        @RequestParam(required = false) String name,
        Pageable pageable
    ) {
        Page<AuthorityResponse> page = authorityService
            .searchActiveAuthorities(name, pageable);

        return ApiResponse.paged(
            HttpStatus.OK, "Authorities retrieved",
            page.getContent(),
            page.getNumber() + 1,
            page.getSize(),
            page.getTotalElements(),
            page.hasNext(),
            page.hasPrevious()
        );
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('authority.delete', 'authority.*')")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (id <= 0)
            throw new BadRequestException("Invalid ID: " + id);

        authorityService.delete(id);
        return ApiResponse.success(HttpStatus.OK, "Authority deleted", null);
    }

}
