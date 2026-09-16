package id.my.rascal.employee.internal.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import id.my.rascal.employee.internal.model.enums.EmployeeStatus;

@Entity
@Getter @Setter
@Table(name = "employees")
public class Employee {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_auth_id", unique = true)
    private Long userAuthId;

    @Column(name = "role_name")
    private String roleName;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "phone")
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EmployeeStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;


    public void markActive() {
        this.status = EmployeeStatus.ACTIVE;
    }

    public void markInactive() {
        this.status = EmployeeStatus.INACTIVE;
    }

    public void markSuspended() {
        this.status = EmployeeStatus.SUSPENDED;
    }

}
