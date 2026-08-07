package id.my.rascal.auth.internal.model.mapper;

import id.my.rascal.auth.internal.entity.UserAuth;
import id.my.rascal.auth.internal.model.request.UserAuthPatchRequest;
import id.my.rascal.auth.internal.model.request.UserAuthPutRequest;
import id.my.rascal.auth.internal.model.request.UserAuthRequest;
import id.my.rascal.auth.internal.model.response.UserAuthResponse;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.stream.Collectors;

public class UserAuthMapper {

    private UserAuthMapper() { }

    public static UserAuth toEntity(UserAuthRequest request) {
        if (request == null) return null;
        
        UserAuth userAuth = new UserAuth();
        userAuth.setEmail(request.email());
        userAuth.setCreatedAt(LocalDateTime.now());
        return userAuth;
    }

    public static UserAuthResponse toResponse(UserAuth userAuth) {
        if (userAuth == null) return null;

        return new UserAuthResponse(
            userAuth.getId(),
            userAuth.getEmail(),
            userAuth.getRoles() != null 
                ? userAuth.getRoles().stream().map(RoleMapper::toResponse).collect(Collectors.toSet())
                : Collections.emptySet(),
            userAuth.getCreatedAt(),
            userAuth.getUpdatedAt()
        );
    }

    public static void updateEntity(UserAuth userAuth, UserAuthPutRequest request) {
        if (request == null || userAuth == null) return;
        
        userAuth.setEmail(request.email());
        userAuth.setUpdatedAt(LocalDateTime.now());
    }

    public static void updateEntity(UserAuth userAuth, UserAuthPatchRequest request) {
        if (request == null || userAuth == null) return;
        
        if (request.email() != null && request.email().isPresent()) {
            userAuth.setEmail(request.email().get());
        }
        userAuth.setUpdatedAt(LocalDateTime.now());
    }
}
