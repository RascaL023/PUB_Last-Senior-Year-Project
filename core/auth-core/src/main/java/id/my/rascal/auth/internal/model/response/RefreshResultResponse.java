package id.my.rascal.auth.internal.model.response;

public record RefreshResultResponse(
    Long userId,
    String email,
    String accessToken,
    String rawRefreshToken
) {}

