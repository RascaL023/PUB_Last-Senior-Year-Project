package id.my.rascal.auth.internal.controller;

import id.my.rascal.auth.internal.model.request.AuthorityPatchRequest;
import id.my.rascal.auth.internal.model.request.AuthorityPutRequest;
import id.my.rascal.auth.internal.model.request.AuthorityRequest;
import id.my.rascal.auth.internal.model.response.AuthorityResponse;
import id.my.rascal.auth.internal.service.AuthorityService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/authorities")
public class AuthorityController {

    private final AuthorityService authorityService;

    public AuthorityController(AuthorityService authorityService) {
        this.authorityService = authorityService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AuthorityResponse create(@RequestBody AuthorityRequest request) {
        return authorityService.create(request);
    }

    @GetMapping("/{id}")
    public AuthorityResponse getById(@PathVariable Long id) {
        return authorityService.getById(id);
    }

    @GetMapping
    public List<AuthorityResponse> getAll() {
        return authorityService.getAll();
    }

    @PutMapping("/{id}")
    public AuthorityResponse updatePut(@PathVariable Long id, @RequestBody AuthorityPutRequest request) {
        return authorityService.update(id, request);
    }

    @PatchMapping("/{id}")
    public AuthorityResponse updatePatch(@PathVariable Long id, @RequestBody AuthorityPatchRequest request) {
        return authorityService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        authorityService.delete(id);
    }
}
