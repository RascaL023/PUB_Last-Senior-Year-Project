package id.my.rascal.employee.internal.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import id.my.rascal.employee.internal.entity.Employee;
import id.my.rascal.employee.internal.model.enums.EmployeeStatus;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    @Query("""
        select e from Employee e
        where (:keyword is null or lower(e.name) like lower(concat('%', cast(:keyword as string), '%'))
            or lower(e.email) like lower(concat('%', cast(:keyword as string), '%')))
          and (:status is null or e.status = :status)
          and (:includeDeleted = true or e.deletedAt is null)
    """)
    Page<Employee> search(
        @Param("keyword") String keyword,
        @Param("status") EmployeeStatus status,
        @Param("includeDeleted") boolean includeDeleted,
        Pageable pageable
    );

    @Query("select e from Employee e where e.id = :id and e.deletedAt is null")
    Optional<Employee> findActiveById(@Param("id") Long id);

    @Query("select e from Employee e where e.userAuthId = :userAuthId and e.deletedAt is null")
    Optional<Employee> findActiveByUserAuthId(@Param("userAuthId") Long userAuthId);

    @Query("select e from Employee e where e.id in :ids")
    List<Employee> findAllByIds(@Param("ids") Collection<Long> ids);

    boolean existsByEmail(String email);

    @Query("select e from Employee e where e.email = :email and e.deletedAt is null")
    Optional<Employee> findByEmail(@Param("email") String email);

}
