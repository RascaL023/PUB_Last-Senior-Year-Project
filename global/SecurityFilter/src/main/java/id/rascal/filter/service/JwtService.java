package id.rascal.filter.service;

import java.util.Date;
import java.util.Set;

import javax.crypto.SecretKey;

import id.rascal.filter.inteface.KeyProvider;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

public class JwtService {

    private final KeyProvider keyProvider;

    public JwtService(KeyProvider keyProvider) {
        this.keyProvider = keyProvider;
    }

    public String generateToken(
        String identifier,
        Set<String> roles,
        Set<String> authorities
    ) {
        String kid = keyProvider.getActiveKid();
        SecretKey secKey = keyProvider.getSecretKeyByKid(kid);

        return Jwts.builder()
            .header().keyId(kid)
            .and()
            .claim("roles", roles)
            .claim("authorities", authorities)
            .subject(identifier)
            .issuedAt(new Date())
            .expiration(keyProvider.getExpirationTimeout())
            .signWith(secKey)
            .compact();
    }

    public Claims extractAllClaims(String token) {
        String kid = keyProvider.getActiveKid();
        SecretKey secKey = keyProvider.getSecretKeyByKid(kid);

        return Jwts.parser()
            .verifyWith(secKey).build()
            .parseSignedClaims(token)
            .getPayload();
    }
    
}
