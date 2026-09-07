package id.my.rascal.customer.internal.repository;

import id.my.rascal.customer.internal.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    @Query("select c from Customer c where c.deletedAt is null and c.id = :id")
    Optional<Customer> findActiveById(@Param("id") Long id);

    @Query("select c from Customer c where c.deletedAt is null and c.userAuthId = :userAuthId")
    Optional<Customer> findActiveByUserAuthId(@Param("userAuthId") Long userAuthId);

    @Query("select case when count(c) > 0 then true else false end from Customer c where c.deletedAt is null and c.id = :id")
    boolean existsActiveById(@Param("id") Long id);

    @Query("""
        select c.id from Customer c
        where c.deletedAt is null
        and (
            :keyword is null or :keyword = ''
            or lower(c.name) like lower(concat('%', cast(:keyword as string), '%'))
            or lower(c.email) like lower(concat('%', cast(:keyword as string), '%'))
            or lower(c.phone) like lower(concat('%', cast(:keyword as string), '%'))
        )
    """)
    Page<Long> findSearchIds(@Param("keyword") String keyword, Pageable pageable);

    @Query("select c from Customer c where c.id in :ids and c.deletedAt is null")
    List<Customer> findAllActiveByIds(@Param("ids") Collection<Long> ids);

}
