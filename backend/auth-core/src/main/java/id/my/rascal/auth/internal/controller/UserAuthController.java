package id.my.rascal.auth.internal.controller;

import id.my.rascal.auth.internal.model.request.UserAuthPatchRequest;
import id.my.rascal.auth.internal.model.request.UserAuthPutRequest;
import id.my.rascal.auth.internal.model.request.UserAuthRequest;
import id.my.rascal.auth.internal.model.response.UserAuthResponse;
import id.my.rascal.auth.internal.service.UserAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserAuthController {

    private final UserAuthService userAuthService;

    public UserAuthController(UserAuthService userAuthService) {
        this.userAuthService = userAuthService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserAuthResponse create(@RequestBody UserAuthRequest request) {
        return userAuthService.create(request);
    }

    @GetMapping("/{id}")
    public UserAuthResponse getById(@PathVariable Long id) {
        return userAuthService.getById(id);
    }

    @GetMapping
    public List<UserAuthResponse> getAll() {
        return userAuthService.getAll();
    }

    @PutMapping("/{id}")
    public UserAuthResponse updatePut(@PathVariable Long id, @RequestBody UserAuthPutRequest request) {
        return userAuthService.update(id, request);
    }

    @PatchMapping("/{id}")
    public UserAuthResponse updatePatch(@PathVariable Long id, @RequestBody UserAuthPatchRequest request) {
        return userAuthService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        userAuthService.delete(id);
    }
}
