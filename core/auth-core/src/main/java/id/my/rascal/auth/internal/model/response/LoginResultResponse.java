package id.my.rascal.auth.internal.model.response;

public record LoginResultResponse(
    Long userId,
    String email,
    String accessToken,
    String rawRefreshToken
) {}
