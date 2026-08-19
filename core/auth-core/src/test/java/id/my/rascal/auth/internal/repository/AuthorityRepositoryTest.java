package id.my.rascal.auth.internal.repository;

import id.my.rascal.auth.internal.entity.Authority;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class AuthorityRepositoryTest {

    @Autowired
    private AuthorityRepository authorityRepository;

    @Test
    void shouldSaveAndFindAuthority() {
        // Arrange
        Authority authority = new Authority();
        authority.setName("ROLE_READ");
        authority.setCreatedAt(LocalDateTime.now());

        // Act
        Authority saved = authorityRepository.save(authority);
        Optional<Authority> found = authorityRepository.findById(saved.getId());

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("ROLE_READ");
        assertThat(found.get().getCreatedAt()).isNotNull();
    }
}
