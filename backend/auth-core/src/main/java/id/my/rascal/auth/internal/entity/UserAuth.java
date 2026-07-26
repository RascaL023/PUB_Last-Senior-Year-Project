package id.my.rascal.auth.internal.entity;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
@Table(name = "users_auth")
public class UserAuth {

    @Id
    @Column(name = "id", nullable = true)
    private Long id;

    @Column(name = "email", nullable = false)
    private String email;
    
    @Column(name = "hashed_password")
    private String hashedPassword;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @ManyToMany(mappedBy = "users")
    private Set<Role> roles = new HashSet<>();
    
}
