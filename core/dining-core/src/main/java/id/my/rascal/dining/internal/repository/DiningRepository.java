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
        order by d.createdAt desc
    """)
    Page<Dining> findAllPaged(Pageable pageable);

    @Query("select d from Dining d where d.id in :ids")
    List<Dining> findAllByIds(@Param("ids") List<Long> ids);

}
