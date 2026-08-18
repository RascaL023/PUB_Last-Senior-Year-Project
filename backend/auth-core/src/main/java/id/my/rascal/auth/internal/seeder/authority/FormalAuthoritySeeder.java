package id.my.rascal.auth.internal.seeder.authority;

import java.time.LocalDateTime;

import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.auth.internal.entity.Authority;
import id.my.rascal.auth.internal.repository.AuthorityRepository;
import id.my.rascal.common.seed.ChunkedSeederSupport;
import id.my.rascal.common.seed.Seeder;

@Component
@Profile("formal-seed")
@Order(10)
public class FormalAuthoritySeeder implements Seeder {

    private final AuthorityRepository authorityRepository;
    private final ChunkedSeederSupport seedSupport;

    public FormalAuthoritySeeder(
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
            AuthorityCatalog.ALL,
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
