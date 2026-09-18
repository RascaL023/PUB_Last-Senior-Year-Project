package id.my.rascal.dining.internal.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import id.my.rascal.dining.internal.entity.Dining;
import id.my.rascal.dining.internal.entity.DiningStatus;

@Repository
public interface DiningRepository extends JpaRepository<Dining, Long> {

    @Query("select d from Dining d where d.id = :id")
    Optional<Dining> findById(@Param("id") Long id);

    @Query("select d from Dining d where d.tableId = :tableId and d.status = :status")
    Optional<Dining> findByTableIdAndStatus(
        @Param("tableId") Long tableId,
        @Param("status") DiningStatus status
    );

    @Query("select case when count(d) > 0 then true else false end from Dining d where d.tableId = :tableId and d.status = :status")
    boolean existsByTableIdAndStatus(
        @Param("tableId") Long tableId,
        @Param("status") DiningStatus status
    );

    @Query("""
        select d from Dining d
        where (:status is null or d.status = :status)
    """)
    Page<Dining> findAllPaged(
        @Param("status") DiningStatus status,
        Pageable pageable
    );

    @Query("select d from Dining d where d.id in :ids")
    List<Dining> findAllByIds(@Param("ids") List<Long> ids);

    @Query("select d from Dining d where d.guestToken = :guestToken")
    Optional<Dining> findByGuestToken(@Param("guestToken") String guestToken);

    @Query("select d from Dining d where d.guestCode = :guestCode and d.status = :status")
    Optional<Dining> findByGuestCodeAndStatus(
        @Param("guestCode") String guestCode,
        @Param("status") DiningStatus status
    );

    @Query("select case when count(d) > 0 then true else false end from Dining d where d.guestCode = :guestCode")
    boolean existsByGuestCode(@Param("guestCode") String guestCode);

}
