package id.my.rascal.auth.internal.seeder.role;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.auth.internal.entity.Role;
import id.my.rascal.auth.internal.repository.AuthorityRepository;
import id.my.rascal.auth.internal.repository.RoleRepository;
import id.my.rascal.auth.internal.seeder.authority.AuthorityCatalog;
import id.my.rascal.common.seed.ChunkedSeederSupport;
import id.my.rascal.common.seed.Seeder;

@Component
@Profile("formal-seed")
@Order(20)
public class FormalRoleSeeder implements Seeder {

    private static final List<RoleSeed> ROLES = List.of(
        new RoleSeed(
            "admin",
            "Owner / administrator with full access",
            AuthorityCatalog.names()
        )
    );

    private final RoleRepository roleRepository;
    private final AuthorityRepository authorityRepository;
    private final ChunkedSeederSupport seedSupport;

    public FormalRoleSeeder(
        RoleRepository roleRepository,
        AuthorityRepository authorityRepository,
        ChunkedSeederSupport seedSupport
    ) {
        this.roleRepository = roleRepository;
        this.authorityRepository = authorityRepository;
        this.seedSupport = seedSupport;
    }

    @Override
    @Transactional
    public void seed() {
        LocalDateTime now = LocalDateTime.now();

        seedSupport.seedInChunks(
            ROLES,
            RoleSeed::name,
            item -> {
                Role role = new Role();
                role.setName(item.name().toUpperCase());
                role.setDescription(item.description());
                role.setCreatedAt(now);
                role.setAuthorities(new HashSet<>(authorityRepository.findAllByNameIn(item.authorityNames())));
                return role;
            },
            roleRepository::findExistingNames,
            roleRepository::saveAll
        );
    }

}
