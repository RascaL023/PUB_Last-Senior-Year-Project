package id.my.rascal.auth.internal.service;

import java.time.Duration;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenCookieFactory {

    public ResponseCookie create(String refreshToken) {
        return ResponseCookie
                .from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(false)
                .sameSite("Strict")
                .path("/api/v1/auths")
                .maxAge(Duration.ofDays(20))
                .build();
    }

    public ResponseCookie clear() {
        return ResponseCookie
                .from("refresh_token", "")
                .httpOnly(true)
                .secure(false)
                .sameSite("Strict")
                .path("/api/v1/auths")
                .maxAge(Duration.ZERO)
                .build();
    }
}
