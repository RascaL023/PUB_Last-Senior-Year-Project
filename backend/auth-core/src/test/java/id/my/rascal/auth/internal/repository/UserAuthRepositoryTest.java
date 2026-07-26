package id.my.rascal.auth.internal.repository;

import id.my.rascal.auth.internal.entity.UserAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class UserAuthRepositoryTest {

    @Autowired
    private UserAuthRepository userAuthRepository;

    @Test
    void shouldFindByEmail() {
        // Arrange
        UserAuth user = new UserAuth();
        user.setId(1L);
        user.setEmail("test@rascal.my.id");
        user.setHashedPassword("hashed123");
        user.setCreatedAt(LocalDateTime.now());
        userAuthRepository.save(user);

        // Act
        Optional<UserAuth> found = userAuthRepository.findByEmail("test@rascal.my.id");
        Optional<UserAuth> notFound = userAuthRepository.findByEmail("nobody@rascal.my.id");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@rascal.my.id");
        assertThat(found.get().getHashedPassword()).isEqualTo("hashed123");
        assertThat(notFound).isEmpty();
    }
}
