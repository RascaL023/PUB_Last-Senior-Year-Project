package id.my.rascal.common.seed;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Runs seed data when a seed type is requested, either through the
 * {@code --seed <dev|formal>} program argument or the legacy seed profiles
 * ({@code dev-seed} / {@code formal-seed}). The flag takes precedence over a
 * profile. Seeders of the resolved type run ordered by {@code @Order}.
 */
@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final List<Seeder> seeders;
    private final ApplicationArguments applicationArguments;
    private final Environment environment;

    public DatabaseSeeder(
        List<Seeder> seeders,
        ApplicationArguments applicationArguments,
        Environment environment
    ) {
        this.seeders = seeders;
        this.applicationArguments = applicationArguments;
        this.environment = environment;
    }

    @Override
    public void run(String... runArgs) {
        SeedType type = resolveSeedType();
        if (type == null) {
            return;
        }

        List<Seeder> selected = new ArrayList<>();
        for (Seeder seeder : seeders) {
            if (seeder.seedType() == type) {
                selected.add(seeder);
            }
        }
        selected.sort(AnnotationAwareOrderComparator.INSTANCE);
        selected.forEach(Seeder::seed);
    }

    private SeedType resolveSeedType() {
        List<String> seedOptions = applicationArguments.getOptionValues("seed");
        if (seedOptions != null) {
            for (String value : seedOptions) {
                SeedType type = SeedType.from(value);
                if (type != null) {
                    return type;
                }
            }
        }
        for (String arg : applicationArguments.getNonOptionArgs()) {
            SeedType type = SeedType.from(arg);
            if (type != null) {
                return type;
            }
        }
        return resolveFromProfiles();
    }

    private SeedType resolveFromProfiles() {
        String[] profiles = environment.getActiveProfiles();
        if (profiles.length == 0) {
            profiles = environment.getDefaultProfiles();
        }
        return Arrays.stream(profiles)
            .map(SeedType::from)
            .filter(java.util.Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

}