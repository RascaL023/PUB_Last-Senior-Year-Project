package id.my.rascal.auth.internal.seeder.authority;

import java.util.List;
import java.util.stream.Stream;

public final class AuthorityCatalog {

    private AuthorityCatalog() {
    }

    public static final List<AuthoritySeed> ALL = Stream.of(
            entity("user", "user"),
            entity("role", "role"),
            entity("authority", "authority"),
            entity("menu", "menu"),
            entity("menu-category", "menu category"),
            entity("modifier", "modifier"),
            List.of(
                new AuthoritySeed("image.create", "Can create image"),
                new AuthoritySeed("image.read", "Can read image"),
                new AuthoritySeed("image.delete", "Can delete image"),
                new AuthoritySeed("image.*", "Have all authorities to image")
            ),
            entity("order", "order"),
            List.of(
                new AuthoritySeed("payment.create", "Can create payment"),
                new AuthoritySeed("payment.read", "Can read payment"),
                new AuthoritySeed("payment.update", "Can update payment"),
                new AuthoritySeed("payment.*", "Have all authorities to payment")
            ),
            entity("customer", "customer"),
            entity("table", "table"),
            List.of(
                new AuthoritySeed("kitchen.read", "Can read kitchen"),
                new AuthoritySeed("kitchen.update", "Can update kitchen"),
                new AuthoritySeed("kitchen.*", "Have all authorities to kitchen")
            ),
            List.of(
                new AuthoritySeed("report.read", "Can read report")
            )
        )
        .flatMap(List::stream)
        .toList();

    public static List<String> names() {
        return ALL.stream().map(AuthoritySeed::name).toList();
    }

    private static List<AuthoritySeed> entity(String key, String label) {
        return List.of(
            new AuthoritySeed(key + ".create", "Can create " + label),
            new AuthoritySeed(key + ".read", "Can read " + label),
            new AuthoritySeed(key + ".update", "Can update " + label),
            new AuthoritySeed(key + ".delete", "Can delete " + label),
            new AuthoritySeed(key + ".*", "Have all authorities to " + label)
        );
    }

}
