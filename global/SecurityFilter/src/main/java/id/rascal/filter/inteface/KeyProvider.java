package id.rascal.filter.inteface;

import java.util.Date;

import javax.crypto.SecretKey;

import io.jsonwebtoken.security.Keys;

public interface KeyProvider {
    default String getActiveKid() {
        return "atlanta";
    }

    default SecretKey getSecretKeyByKid(String kid) {
        return Keys.hmacShaKeyFor(
            "w9hf20HADJIOasd9283hads9g2yeh29d82uasdh9asdh9".getBytes()
        );
    }

    default Date getExpirationTimeout() {
        return new Date(System.currentTimeMillis() + 1000 * 60 * 15);
    }
}
