package id.my.rascal.auth.internal.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import id.my.rascal.auth.internal.entity.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

}
