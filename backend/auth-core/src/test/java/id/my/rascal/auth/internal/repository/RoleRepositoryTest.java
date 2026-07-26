package id.my.rascal.auth.internal.repository;

import id.my.rascal.auth.internal.entity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class RoleRepositoryTest {

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void shouldFindByName() {
        // Arrange
        Role role = new Role();
        role.setName("ADMIN");
        role.setCreatedAt(LocalDateTime.now());
        roleRepository.save(role);

        // Act
        Optional<Role> found = roleRepository.findByName("ADMIN");
        Optional<Role> notFound = roleRepository.findByName("USER");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("ADMIN");
        assertThat(notFound).isEmpty();
    }
}
