package id.my.rascal.auth.api;

public interface AuthClient {
    String login(String username, String password);
}