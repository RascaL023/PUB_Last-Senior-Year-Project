package id.my.rascal.auth.internal.seeder.user;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.auth.internal.entity.UserAuth;
import id.my.rascal.auth.internal.repository.RoleRepository;
import id.my.rascal.auth.internal.repository.UserAuthRepository;
import id.my.rascal.common.seed.ChunkedSeederSupport;
import id.my.rascal.common.seed.Seeder;
import id.my.rascal.common.seed.SeedType;

@Component
@Order(30)
public class DevUserSeeder implements Seeder {

    private static final List<UserSeed> USERS = List.of(
        new UserSeed("admin@rascal.id", "admin123", List.of("ADMIN")),
        new UserSeed("kasir@rascal.id", "kasir123", List.of("CASHIER")),
        new UserSeed("waiter@rascal.id", "waiter123", List.of("WAITER")),
        new UserSeed("kitchen@rascal.id", "kitchen123", List.of("KITCHEN"))
    );

    private final UserAuthRepository userAuthRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final ChunkedSeederSupport seedSupport;

    public DevUserSeeder(
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
    public SeedType seedType() {
        return SeedType.DEV;
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
