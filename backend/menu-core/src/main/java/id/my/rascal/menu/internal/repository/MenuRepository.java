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

    // ---- v1: seleksi id yang cocok dengan filter (ringan, tanpa relasi) ----

    /**
     * Ambil halaman id menu yang memenuhi filter (search by name, by category,
     * dan state deleted via showDeleted). Relasi tidak di-fetch di sini, hanya
     * dipakai untuk filtering; DISTINCT menghindari id ganda dari join.
     */
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

    // ---- v2: muat entitas penuh beserta relasi (anti N+1 via @BatchSize) ----

    /**
     * Muat beberapa menu sekaligus dari daftar id. Relasi (categories,
     * modifierTypes, modifierOptions) tidak di-join-fetch di sini melainkan
     * di-batch-load oleh @BatchSize saat diakses, sehingga tidak ada lazy load
     * per-baris dan tidak ada Cartesian product/MultipleBagFetchException.
     */
    @Query("select m from Menu m where m.id in :ids")
    List<Menu> findAllWithRelationsByIds(@Param("ids") Collection<Long> ids);

    /**
     * Muat satu menu, memperhatikan state deleted. Relasi ter-batch-load saat
     * diakses (menghindari N+1 saat merender response).
     */
    @Query("""
        select m from Menu m
        where m.id = :id
          and ((:showDeleted = true and m.deletedAt is not null) or (:showDeleted = false and m.deletedAt is null))
    """)
    Optional<Menu> findWithRelationsById(@Param("id") Long id, @Param("showDeleted") boolean showDeleted);

    // ---- proyeksi ringan untuk cache FE / versioning ----

    /**
     * Proyeksi id yang relevan (menu, category, modifierType) untuk daftar
     * ringan yang dikirim ke FE sebagai penanda perubahan (versioning).
     */
    @Query("""
        select distinct m.id as menuId, c.id as categoryId, mt.id as modifierTypeId
        from Menu m
        left join m.categories c
        left join m.modifierTypes mt
        where m.id in :ids
          and ((:showDeleted = true and m.deletedAt is not null) or (:showDeleted = false and m.deletedAt is null))
    """)
    List<MenuIdView> findIdViews(@Param("ids") Collection<Long> ids, @Param("showDeleted") boolean showDeleted);

    // ---- util ----

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

}
