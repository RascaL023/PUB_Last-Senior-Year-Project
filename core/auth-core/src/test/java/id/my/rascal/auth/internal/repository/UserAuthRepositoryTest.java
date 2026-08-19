package id.my.rascal.auth.internal.repository;

import id.my.rascal.auth.internal.entity.UserAuth;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class UserAuthRepositoryTest {

    @Autowired
    private UserAuthRepository userAuthRepository;

    private long nextId = 1L;

    private UserAuth createUser(String email) {
        UserAuth user = new UserAuth();
        user.setId(nextId++);
        user.setEmail(email);
        user.setHashedPassword("hashed123");
        user.setCreatedAt(LocalDateTime.now());
        return userAuthRepository.save(user);
    }

    @Test
    void shouldFindByEmail() {
        UserAuth user = createUser("test@rascal.my.id");

        Optional<UserAuth> found = userAuthRepository.findByEmail("test@rascal.my.id");
        Optional<UserAuth> notFound = userAuthRepository.findByEmail("nobody@rascal.my.id");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("test@rascal.my.id");
        assertThat(notFound).isEmpty();
    }

    @Test
    void shouldFindActiveById_whenNotDeleted() {
        UserAuth user = createUser("active@rascal.my.id");

        Optional<UserAuth> found = userAuthRepository.findActiveById(user.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("active@rascal.my.id");
    }

    @Test
    void shouldNotFindActiveById_whenDeleted() {
        UserAuth user = createUser("deleted@rascal.my.id");
        user.setDeletedAt(LocalDateTime.now());
        userAuthRepository.save(user);

        Optional<UserAuth> found = userAuthRepository.findActiveById(user.getId());

        assertThat(found).isEmpty();
    }

    @Test
    void shouldFindActiveByEmail_whenNotDeleted() {
        UserAuth user = createUser("email@rascal.my.id");

        Optional<UserAuth> found = userAuthRepository.findActiveByEmail("email@rascal.my.id");

        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("email@rascal.my.id");
    }

    @Test
    void shouldNotFindActiveByEmail_whenDeleted() {
        UserAuth user = createUser("deleted-email@rascal.my.id");
        user.setDeletedAt(LocalDateTime.now());
        userAuthRepository.save(user);

        Optional<UserAuth> found = userAuthRepository.findActiveByEmail("deleted-email@rascal.my.id");

        assertThat(found).isEmpty();
    }

    @Test
    void shouldSearchActiveUsersByEmail() {
        createUser("alpha@rascal.my.id");
        createUser("beta@rascal.my.id");
        UserAuth deleted = createUser("deleted@rascal.my.id");
        deleted.setDeletedAt(LocalDateTime.now());
        userAuthRepository.save(deleted);

        Page<UserAuth> result = userAuthRepository.searchActive("@rascal.my.id", Pageable.ofSize(10));

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).extracting(UserAuth::getEmail)
            .containsExactlyInAnyOrder("alpha@rascal.my.id", "beta@rascal.my.id");
    }
}
