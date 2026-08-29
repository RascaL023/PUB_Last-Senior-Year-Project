package id.my.rascal.auth.internal.repository;

import id.my.rascal.auth.internal.entity.UserAuth;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserAuthRepository extends JpaRepository<UserAuth, Long> {
    Optional<UserAuth> findByEmail(String email);

    @Query("""
        select distinct u from UserAuth u
        left join fetch u.roles
        left join fetch u.roles.authorities
        where u.deletedAt is null and lower(u.email) = lower(:email)
        """)
    Optional<UserAuth> findForLoginByEmail(@Param("email") String email);

    @Query("select u.email from UserAuth u where u.email in :emails")
    List<String> findExistingEmails(@Param("emails") Collection<String> emails);

    @Query("select u from UserAuth u where u.deletedAt is null and u.id = :id")
    @EntityGraph(attributePaths = "roles")
    Optional<UserAuth> findActiveById(@Param("id") Long id);

    @Query("select u from UserAuth u where u.deletedAt is null and u.email = :email")
    @EntityGraph(attributePaths = "roles")
    Optional<UserAuth> findActiveByEmail(@Param("email") String email);

    @Query("""
        select u from UserAuth u
        where u.deletedAt is null
        and (lower(u.email) like lower(concat('%', cast(:email as string), '%')))
    """)
    @EntityGraph(attributePaths = "roles")
    Page<UserAuth> searchActive(@Param("email") String email, Pageable pageable);

    @Query("""
        select ua.id  from UserAuth ua
        where (
            :email is null or lower(ua.email) like 
            lower(concat('%', cast(:email as string), '%'))
        ) and (
            (:showDeleted = true and ua.deletedAt is not null) or
            (:showDeleted = false and ua.deletedAt is null)
        ) order by ua.email
    """)
    Page<Long> findSearchIds(@Param("email") String email, @Param("showDeleted") boolean showDeleted, Pageable pageable);

    @Query("select ua from UserAuth ua where ua.id in :ids")
    List<UserAuth> findAllByIds(@Param("ids") Collection<Long> ids);

}
