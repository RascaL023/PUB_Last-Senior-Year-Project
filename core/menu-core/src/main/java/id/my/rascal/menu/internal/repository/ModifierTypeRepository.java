package id.my.rascal.menu.internal.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import id.my.rascal.menu.internal.entity.ModifierType;

public interface ModifierTypeRepository extends JpaRepository<ModifierType, Long> {

    @EntityGraph(attributePaths = "modifierOptions")
    Optional<ModifierType> findById(Long id);

    @Query("""
        select mt from ModifierType mt
        where (lower(mt.name) like lower(concat('%', cast(:name as string), '%')))
    """)
    Page<ModifierType> search(@Param("name") String name, Pageable pageable);


}
