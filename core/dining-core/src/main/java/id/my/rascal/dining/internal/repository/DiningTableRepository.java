package id.my.rascal.dining.internal.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import id.my.rascal.dining.internal.entity.DiningTable;
import id.my.rascal.dining.internal.entity.TableStatus;

@Repository
public interface DiningTableRepository extends JpaRepository<DiningTable, Long> {

    @Query("select t from DiningTable t where t.deletedAt is null and t.id = :id")
    Optional<DiningTable> findActiveById(@Param("id") Long id);

    boolean existsByTableNumber(String tableNumber);

    @Query("""
        select t from DiningTable t
        where t.deletedAt is null
          and (:keyword is null or lower(t.tableNumber) like lower(concat('%', cast(:keyword as string), '%')))
        order by t.tableNumber asc
    """)
    Page<DiningTable> searchActive(@Param("keyword") String keyword, Pageable pageable);

    @Query("select t from DiningTable t where t.deletedAt is null and t.status = :status")
    List<DiningTable> findAllActiveByStatus(@Param("status") TableStatus status);

    @Query("select t from DiningTable t where t.deletedAt is null and t.id in :ids")
    List<DiningTable> findActiveByIds(@Param("ids") Collection<Long> ids);

}
