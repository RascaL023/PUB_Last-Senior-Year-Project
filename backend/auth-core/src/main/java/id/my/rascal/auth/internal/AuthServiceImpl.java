package id.my.rascal.auth.internal;

import id.my.rascal.auth.api.AuthClient;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthClient {
    @Override
    public String login(String username, String password) {
        return "Token untuk " + username;
    }
}
