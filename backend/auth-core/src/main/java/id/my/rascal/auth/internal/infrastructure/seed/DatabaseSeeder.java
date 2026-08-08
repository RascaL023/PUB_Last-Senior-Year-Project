package id.my.rascal.auth.internal.infrastructure.seed;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import id.my.rascal.auth.internal.seeder.Seeder;

@Component
@Profile({"dev-seed", "formal-seed"})
public class DatabaseSeeder implements CommandLineRunner {

    private final List<Seeder> seeders;

    public DatabaseSeeder(List<Seeder> seeders) {
        this.seeders = seeders;
    }

    @Override
    public void run(String... args) throws Exception {
        seeders.forEach(Seeder::seed);
    }
    
}
