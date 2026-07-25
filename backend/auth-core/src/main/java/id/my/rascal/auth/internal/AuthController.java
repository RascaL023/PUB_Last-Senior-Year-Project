package id.my.rascal.auth.internal;

import id.my.rascal.auth.api.AuthClient;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    
    private final AuthClient authClient;

    public AuthController(AuthClient authClient) {
        this.authClient = authClient;
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password) {
        return authClient.login(username, password);
    }
}
