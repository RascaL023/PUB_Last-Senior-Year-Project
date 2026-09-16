package id.my.rascal.auth.internal.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import id.my.rascal.auth.internal.entity.PasswordResetToken;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    List<PasswordResetToken> findByExpiresAtAfterAndRevokedAtIsNull(Instant now);
    
    Optional<PasswordResetToken> findFirstByUserAuthId(Long userId);

    @Modifying
    @Query("""
        delete from PasswordResetToken t
        where t.userAuth.id = :userId
    """)
    void deleteByUserAuthId(@Param("userId") Long userId);

}
