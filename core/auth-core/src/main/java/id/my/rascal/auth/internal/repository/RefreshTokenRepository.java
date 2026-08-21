package id.my.rascal.auth.internal.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import id.my.rascal.auth.internal.entity.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
        update RefreshToken rt set
            rt.revokedAt = :now
        where rt.userAuth.id = :userId and rt.revokedAt is null
    """)
    void revokeAllByUserId(@Param("userId") Long userId, @Param("now") Instant now);

}
