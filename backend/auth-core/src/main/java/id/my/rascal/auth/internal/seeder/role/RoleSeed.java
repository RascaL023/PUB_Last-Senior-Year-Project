package id.my.rascal.auth.internal.seeder.role;

import java.util.List;

public record RoleSeed(String name, String description, List<String> authorityNames) {

}
