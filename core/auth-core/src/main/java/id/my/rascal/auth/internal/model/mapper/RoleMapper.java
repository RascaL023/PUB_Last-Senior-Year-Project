package id.my.rascal.auth.internal.model.mapper;

import id.my.rascal.auth.internal.entity.Role;
import id.my.rascal.auth.internal.model.request.RolePatchRequest;
import id.my.rascal.auth.internal.model.request.RolePutRequest;
import id.my.rascal.auth.internal.model.request.RoleRequest;
import id.my.rascal.auth.internal.model.response.RoleResponse;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.stream.Collectors;

public class RoleMapper {

    private RoleMapper() {
        // utility class
    }

    public static Role toEntity(RoleRequest request) {
        if (request == null) return null;
        
        Role role = new Role();
        role.setName(normalizeRoleName(request.name()));
        role.setCreatedAt(LocalDateTime.now());
        return role;
    }

    public static RoleResponse toResponse(Role role) {
        if (role == null) return null;

        return new RoleResponse(
            role.getId(),
            role.getName(),
            role.getAuthorities() != null 
                ? role.getAuthorities().stream().map(AuthorityMapper::toResponse).collect(Collectors.toSet())
                : Collections.emptySet(),
            role.getCreatedAt(),
            role.getUpdatedAt()
        );
    }

    public static void updateEntity(Role role, RolePutRequest request) {
        if (request == null || role == null) return;
        
        role.setName(normalizeRoleName(request.name()));
        role.setUpdatedAt(LocalDateTime.now());
    }

    public static void updateEntity(Role role, RolePatchRequest request) {
        if (request == null || role == null) return;
        
        if (request.name() != null && request.name().isPresent()) {
            role.setName(normalizeRoleName(request.name().get()));
            role.setUpdatedAt(LocalDateTime.now());
        }
    }

    private static String normalizeRoleName(String role) {
        return role.trim().toUpperCase();
    }

}
