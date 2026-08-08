package id.my.rascal.auth.internal.seeder.authority;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.auth.internal.entity.Authority;
import id.my.rascal.auth.internal.repository.AuthorityRepository;
import id.my.rascal.auth.internal.seeder.Seeder;

@Component
@Profile("formal-seed")
public class FormalAuthoritySeeder implements Seeder {

    private static final Map<String, String> AUTHORITIES = Map.of(
        "user.create", "Can create user",
        "user.read", "Can read user",
        "user.update", "Can update user",
        "user.delete", "Can delete user",
        "user.*", "Have all authorities to user"
    );

    private final AuthorityRepository authorityRepository;

    public FormalAuthoritySeeder(AuthorityRepository authorityRepository) {
        this.authorityRepository = authorityRepository;
    }

    @Override
    @Transactional
    public void seed() {
        LocalDateTime now = LocalDateTime.now();

        Set<String> existingNames = new HashSet<>(
            authorityRepository.findExistingNames(AUTHORITIES.keySet())
        );

        List<Authority> authorities = AUTHORITIES.entrySet()
            .stream()
            .filter(entry -> !existingNames.contains(entry.getKey()))
            .map(entry -> {
                Authority authority = new Authority();
                authority.setName(entry.getKey());
                authority.setDescription(entry.getValue());
                authority.setCreatedAt(now);
                return authority;
            })
            .toList();

        authorityRepository.saveAll(authorities);
    }

}

