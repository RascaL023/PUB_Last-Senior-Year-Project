package id.my.rascal.dining.internal.service;

import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Component;

@Component
public class GuestTokenGenerator {

    private static final int TOKEN_BYTES = 32;
    private static final int CODE_BOUND = 1_000_000; // 000000..999999

    private final SecureRandom secureRandom = new SecureRandom();

    public String nextToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String nextCode() {
        return String.format("%06d", secureRandom.nextInt(CODE_BOUND));
    }

}
