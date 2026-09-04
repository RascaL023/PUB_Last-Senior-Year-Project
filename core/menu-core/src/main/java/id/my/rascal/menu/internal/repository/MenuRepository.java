package id.my.rascal.menu.internal.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import id.my.rascal.menu.internal.entity.Menu;

public interface MenuRepository extends JpaRepository<Menu, Long> {

    // ---- v1: get all, no cached on FE ----

    @Query("""
        select m.id
        from Menu m
        where (:name is null or lower(m.name) like lower(concat('%', cast(:name as string), '%')))
          and (:categoryId is null or exists (
              select 1 from m.categories c where c.id = :categoryId
          ))
          and ((:showDeleted = true and m.deletedAt is not null) or (:showDeleted = false and m.deletedAt is null))
        order by m.name
    """)
    Page<Long> findSearchIds(
        @Param("name") String name,
        @Param("categoryId") Long categoryId,
        @Param("showDeleted") boolean showDeleted,
        Pageable pageable
    );

    @Query("select m from Menu m where m.id in :ids")
    List<Menu> findAllByIds(@Param("ids") Collection<Long> ids);

    @Query("""
        select m from Menu m
        where m.id = :id
          and ((:showDeleted = true and m.deletedAt is not null) or (:showDeleted = false and m.deletedAt is null))
    """)
    Optional<Menu> findWithRelationsById(@Param("id") Long id, @Param("showDeleted") boolean showDeleted);

    // ---- v2: proyeksi ringan untuk cache FE / versioning ----

    @Query("""
        select distinct m.id as menuId, c.id as categoryId, mt.id as modifierTypeId
        from Menu m
        left join m.categories c
        left join m.modifierTypes mt
        where m.id in :ids
          and ((:showDeleted = true and m.deletedAt is not null) or (:showDeleted = false and m.deletedAt is null))
    """)
    List<MenuIdView> findViewByIds(@Param("ids") Collection<Long> ids, @Param("showDeleted") boolean showDeleted);

    @Query("""
        select distinct m.id as menuId, c.id as categoryId, mt.id as modifierTypeId
        from Menu m
        left join m.categories c
        left join m.modifierTypes mt
        where m.id = :id
          and ((:showDeleted = true and m.deletedAt is not null) or (:showDeleted = false and m.deletedAt is null))
    """)
    List<MenuIdView> findViewById(@Param("id") Long id, @Param("showDeleted") boolean showDeleted);

    // ---- util ----

    boolean existsByName(String name);

    @Query("""
        select distinct m from Menu m
        join m.modifierTypes mt
        where mt.id = :id
    """)
    List<Menu> findByModifierTypeId(@Param("id") Long id);

    @Query("""
        select distinct m from Menu m
        join m.categories mc
        where mc.id = :id
    """)
    List<Menu> findByCategoryId(@Param("id") Long id);

    @Query("""
        select distinct m from Menu m
        join m.imageUrls iu
        where iu = :imageUrl
    """)
    List<Menu> findByImageUrl(@Param("imageUrl") String imageUrl);

}
