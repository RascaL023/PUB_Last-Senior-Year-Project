package id.my.rascal.auth.internal.seeder.user;

import java.util.List;

public record UserSeed(String email, String rawPassword, List<String> roleNames) {

}
