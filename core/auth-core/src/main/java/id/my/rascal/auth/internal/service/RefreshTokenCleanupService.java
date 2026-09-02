package id.my.rascal.auth.internal.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.auth.internal.repository.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenCleanupService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void cleanupExpiredAndOldRevokedTokens() {
        Instant now = Instant.now();
        Instant revokedBefore = now.minus(7, ChronoUnit.DAYS);

        refreshTokenRepository.deleteExpiredAndOldRevoked(now, revokedBefore);
    }

}
