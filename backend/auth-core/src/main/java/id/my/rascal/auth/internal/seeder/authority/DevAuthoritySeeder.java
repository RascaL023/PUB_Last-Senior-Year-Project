package id.my.rascal.auth.internal.seeder.authority;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.auth.internal.entity.Authority;
import id.my.rascal.auth.internal.repository.AuthorityRepository;
import id.my.rascal.common.seed.ChunkedSeederSupport;
import id.my.rascal.common.seed.Seeder;

@Component
@Profile("dev-seed")
@Order(10)
public class DevAuthoritySeeder implements Seeder {

    private static final List<AuthoritySeed> AUTHORITIES = List.of(
        new AuthoritySeed("user.create", "Can create user"),
        new AuthoritySeed("user.read", "Can read user"),
        new AuthoritySeed("user.update", "Can update user"),
        new AuthoritySeed("user.delete", "Can delete user"),
        new AuthoritySeed("user.*", "Have all authorities to user"),

        new AuthoritySeed("role.create", "Can create role"),
        new AuthoritySeed("role.read", "Can read role"),
        new AuthoritySeed("role.update", "Can update role"),
        new AuthoritySeed("role.delete", "Can delete role"),
        new AuthoritySeed("role.*", "Have all authorities to role")
    );

    private final AuthorityRepository authorityRepository;
    private final ChunkedSeederSupport seedSupport;

    public DevAuthoritySeeder(
        AuthorityRepository authorityRepository, 
        ChunkedSeederSupport seedSupport
    ) {
        this.authorityRepository = authorityRepository;
        this.seedSupport = seedSupport;
    }

    @Override
    @Transactional
    public void seed() {
        LocalDateTime now = LocalDateTime.now();

        seedSupport.seedInChunks(
            AUTHORITIES,
            AuthoritySeed::name,
            item -> {
                Authority authority = new Authority();
                authority.setName(item.name());
                authority.setDescription(item.description());
                authority.setCreatedAt(now);
                return authority;
            },
            authorityRepository::findExistingNames,
            authorityRepository::saveAll
        );
    }

}
