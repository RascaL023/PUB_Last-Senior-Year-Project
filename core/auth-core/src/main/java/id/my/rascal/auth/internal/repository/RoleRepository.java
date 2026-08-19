package id.my.rascal.auth.internal.repository;

import id.my.rascal.auth.internal.entity.Role;

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
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(String name);

    @Query("select r.name from Role r where r.name in :names")
    List<String> findExistingNames(@Param("names") Collection<String> names);

    @Query("""
        select r from Role r
        where r.deletedAt is null and r.name in :names
    """)
    List<Role> findAllByNameIn(@Param("names") Collection<String> names);

    @Query("""
        select r from Role r
        where r.id = :id
            and (:showDeleted = true and r.deletedAt is not null)
            or (:showDeleted = false and r.deletedAt is null)
    """)
    @EntityGraph(attributePaths = "authorities")
    Optional<Role> findById(@Param("id") Long id, @Param("showDeleted") boolean showDeleted);

    @Query("""
        select r from Role r
        where (lower(r.name) like lower(concat('%', cast(:name as string), '%')))
            and (
                (:showDeleted = true and r.deletedAt is not null)
                or (:showDeleted = false and r.deletedAt is null)
            )
    """)
    Page<Role> searchRole(@Param("name") String name, boolean showDeleted, Pageable pageable);

}
