package id.my.rascal.auth.api;

public record CreateAccountRequest(
    String email,
    String password,
    String roleName
) {}
