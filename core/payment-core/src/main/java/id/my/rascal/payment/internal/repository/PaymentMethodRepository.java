package id.my.rascal.payment.internal.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import id.my.rascal.payment.internal.entity.PaymentMethod;

import java.util.Optional;

@Repository
public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    boolean existsByCode(String code);

    boolean existsByName(String name);

    @Query("select m from PaymentMethod m where m.deletedAt is null and lower(m.code) = lower(:code)")
    Optional<PaymentMethod> findByCodeAndDeletedAtIsNull(@Param("code") String code);

    @Query("select m from PaymentMethod m where m.deletedAt is null and m.id = :id")
    Optional<PaymentMethod> findActiveById(@Param("id") Long id);

    @Query("""
        select m from PaymentMethod m
        where m.deletedAt is null
          and (:keyword is null or lower(m.name) like lower(concat('%', cast(:keyword as string), '%'))
               or lower(m.code) like lower(concat('%', cast(:keyword as string), '%')))
        order by m.name asc
    """)
    Page<PaymentMethod> searchActive(@Param("keyword") String keyword, Pageable pageable);

}
