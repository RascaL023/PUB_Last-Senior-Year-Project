package id.my.rascal.menu.internal.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import id.my.rascal.menu.internal.entity.MenuCategory;

public interface MenuCategoryRepository extends JpaRepository<MenuCategory, Long> {

    @Query("""
        select mc from MenuCategory mc
        where mc.id = :id
          and (
            (:showDeleted = true and mc.deletedAt is not null)
            or (:showDeleted = false and mc.deletedAt is null)
          )
    """)
    Optional<MenuCategory> findActiveById(
        @Param("id") Long id, 
        @Param("showDeleted") boolean showDeleted
    );

    @Query("""
        select mc from MenuCategory mc
        where mc.deletedAt is null
          and mc.id in :ids
    """)
    List<MenuCategory> findActiveByIds(@Param("ids") Iterable<Long> ids);

    @Query("""
        select mc from MenuCategory mc
        where (lower(mc.displayName) like lower(concat('%', cast(:displayName as string), '%')))
          and (
            (:showDeleted = true and mc.deletedAt is not null)
            or (:showDeleted = false and mc.deletedAt is null)
          )
    """)
    Page<MenuCategory> searchActiveCategories(
        @Param("displayName") String displayName, 
        @Param("showDeleted") boolean showDeleted,
        Pageable pageable
    );

    boolean existsByCategoryCode(String categoryCode);

}
