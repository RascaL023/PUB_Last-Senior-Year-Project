package id.my.rascal.auth.internal.repository;

import id.my.rascal.auth.internal.entity.Authority;

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
public interface AuthorityRepository extends JpaRepository<Authority, Long> {

    Optional<Authority> findByName(String name);

    @Query("""
        select a from Authority a
        where a.deletedAt is null
        and a.id = :id
    """)
    @EntityGraph(attributePaths = "roles")
    Optional<Authority> findActiveById(@Param("id") Long id);

    @Query("""
        select a from Authority a
        where a.deletedAt is null
        and (lower(a.name) like lower(concat('%', cast(:name as string), '%')))
    """)
    Page<Authority> searchActive(@Param("name") String name, Pageable pageable);

    @Query("""
        select a from Authority a
        where a.deletedAt is null and a.id in :ids
    """)
    List<Authority> findActiveByIds(Iterable<Long> ids);

    @Query("select a.name from Authority a where a.name in :names")
    List<String> findExistingNames(@Param("names") Collection<String> names);

}
