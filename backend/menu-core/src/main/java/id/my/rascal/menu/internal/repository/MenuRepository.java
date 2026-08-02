package id.my.rascal.menu.internal.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import id.my.rascal.menu.internal.entity.Menu;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MenuRepository extends JpaRepository<Menu, Long> {

    @Query("""
        select m from Menu m
        where m.deletedAt is null
        and m.id = :id
    """)
    Optional<Menu> findActiveById(@Param("id") Long id);

    @Query("""
        select m from Menu m
        where m.deletedAt is null
        and (lower(m.name) like lower(concat('%', cast(:name as string), '%')))
    """)
    Page<Menu> searchActiveMenus(@Param("name") String name, Pageable pageable);

    @Query("""
        select m from Menu m
        where m.deletedAt is null
    """)
    Page<Menu> findAllActive(Pageable pageable);

    @Query("""
        select m from Menu m
        where m.deletedAt is not null
    """)
    Page<Menu> findAllDeleted(Pageable pageable);

    @Query("""
        select m from Menu m
        where m.deletedAt is not null
        and m.id = :id
    """)
    Optional<Menu> findDeletedById(@Param("id") Long id);

    @Query("""
        select m from Menu m
        where m.deletedAt is not null
        and (lower(m.name) like lower(concat('%', cast(:name as string), '%')))
    """)
    Page<Menu> searchDeletedMenus(@Param("name") String name, Pageable pageable);

    @Query("""
        select distinct m from Menu m
        join m.modifierTypes mt
        where mt.id = :id
    """)
    List<Menu> findByModifierTypeId(@Param("id") Long id);

}
