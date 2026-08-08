package id.my.rascal.common.seed;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile({"dev-seed", "formal-seed"})
public class DatabaseSeeder implements CommandLineRunner {

    private final List<Seeder> seeders;

    public DatabaseSeeder(List<Seeder> seeders) {
        this.seeders = seeders;
    }

    @Override
    public void run(String... args) {
        seeders.forEach(Seeder::seed);
    }

}
