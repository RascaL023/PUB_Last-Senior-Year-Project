package id.my.rascal.auth.internal.seeder.user;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.auth.internal.entity.UserAuth;
import id.my.rascal.auth.internal.repository.RoleRepository;
import id.my.rascal.auth.internal.repository.UserAuthRepository;
import id.my.rascal.common.seed.ChunkedSeederSupport;
import id.my.rascal.common.seed.Seeder;

@Component
@Profile("formal-seed")
@Order(30)
public class FormalUserSeeder implements Seeder {

    private static final List<UserSeed> USERS = List.of(
        new UserSeed("admin@rascal.id", "admin123", List.of("admin"))
    );

    private final UserAuthRepository userAuthRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final ChunkedSeederSupport seedSupport;

    public FormalUserSeeder(
        UserAuthRepository userAuthRepository,
        RoleRepository roleRepository,
        PasswordEncoder passwordEncoder,
        ChunkedSeederSupport seedSupport
    ) {
        this.userAuthRepository = userAuthRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedSupport = seedSupport;
    }

    @Override
    @Transactional
    public void seed() {
        LocalDateTime now = LocalDateTime.now();

        seedSupport.seedInChunks(
            USERS,
            UserSeed::email,
            item -> {
                UserAuth user = new UserAuth();
                user.setEmail(item.email());
                user.setHashedPassword(passwordEncoder.encode(item.rawPassword()));
                user.setCreatedAt(now);
                user.setRoles(new HashSet<>(roleRepository.findAllByNameIn(item.roleNames())));
                return user;
            },
            userAuthRepository::findExistingEmails,
            userAuthRepository::saveAll
        );
    }

}
