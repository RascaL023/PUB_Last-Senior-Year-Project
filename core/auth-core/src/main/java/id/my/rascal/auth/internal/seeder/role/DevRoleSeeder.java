package id.my.rascal.auth.internal.seeder.role;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.auth.internal.entity.Role;
import id.my.rascal.auth.internal.repository.AuthorityRepository;
import id.my.rascal.auth.internal.repository.RoleRepository;
import id.my.rascal.auth.internal.seeder.authority.AuthorityCatalog;
import id.my.rascal.common.seed.ChunkedSeederSupport;
import id.my.rascal.common.seed.Seeder;
import id.my.rascal.common.seed.SeedType;

@Component
@Order(20)
public class DevRoleSeeder implements Seeder {

    private static final List<RoleSeed> ROLES = List.of(
        new RoleSeed(
            "admin",
            "Full system access",
            AuthorityCatalog.names()
        ),
        new RoleSeed(
            "cashier",
            "Cashier handling orders and payments",
            List.of(
                "order.create", "order.read", "order.update",
                "payment.create", "payment.read", "payment.update",
                "customer.create", "customer.read", "customer.update",
                "menu.read",
                "menu-category.read",
                "menu-modifier.read",
                "image.read",
                "dining.read",
                "table.read",
                "report.read"
            )
        ),
        new RoleSeed(
            "waiter",
            "Waiter serving orders and managing tables",
            List.of(
                "order.create", "order.read", "order.update",
                "order.mark.completed",
                "customer.read",
                "payment.read",
                "menu.read",
                "menu-category.read",
                "menu-modifier.read",
                "image.read",
                "dining.create", "dining.read", "dining.update",
                "table.create", "table.read", "table.update"
            )
        ),
        new RoleSeed(
            "kitchen",
            "Kitchen staff preparing orders",
            List.of(
                "order.read",
                "order.mark.preparing", "order.mark.ready",
                "kitchen.read", "kitchen.update",
                "menu.read",
                "image.read"
            )
        ),
        new RoleSeed(
            "customer_base",
            "Customer login identity without staff permissions",
            List.of()
        )
    );

    private final RoleRepository roleRepository;
    private final AuthorityRepository authorityRepository;
    private final ChunkedSeederSupport seedSupport;

    public DevRoleSeeder(
        RoleRepository roleRepository,
        AuthorityRepository authorityRepository,
        ChunkedSeederSupport seedSupport
    ) {
        this.roleRepository = roleRepository;
        this.authorityRepository = authorityRepository;
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
            ROLES,
            item -> item.name().toUpperCase(),
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
