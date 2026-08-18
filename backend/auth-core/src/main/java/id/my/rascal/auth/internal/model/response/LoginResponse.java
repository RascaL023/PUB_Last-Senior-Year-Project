package id.my.rascal.auth.internal.model.response;

public record LoginResponse(
    Long id,
    String email,
    String accessToken
) { }
