package id.my.rascal.auth.internal.model.mapper;

import id.my.rascal.auth.internal.entity.Authority;
import id.my.rascal.auth.internal.model.request.AuthorityPatchRequest;
import id.my.rascal.auth.internal.model.request.AuthorityPutRequest;
import id.my.rascal.auth.internal.model.request.AuthorityRequest;
import id.my.rascal.auth.internal.model.response.AuthorityResponse;

import java.time.LocalDateTime;

public class AuthorityMapper {

    private AuthorityMapper() {
        // utility class
    }

    public static Authority toEntity(AuthorityRequest request) {
        if (request == null) return null;
        
        Authority authority = new Authority();
        authority.setName(request.name());
        authority.setCreatedAt(LocalDateTime.now());
        return authority;
    }

    public static AuthorityResponse toResponse(Authority authority) {
        if (authority == null) return null;

        return new AuthorityResponse(
            authority.getId(),
            authority.getName(),
            authority.getCreatedAt(),
            authority.getUpdatedAt()
        );
    }

    public static void updateEntity(Authority authority, AuthorityPutRequest request) {
        if (request == null || authority == null) return;
        
        authority.setName(request.name());
        authority.setUpdatedAt(LocalDateTime.now());
    }

    public static void updateEntity(Authority authority, AuthorityPatchRequest request) {
        if (request == null || authority == null) return;
        
        if (request.name() != null && request.name().isPresent()) {
            authority.setName(request.name().get());
            authority.setUpdatedAt(LocalDateTime.now());
        }
    }
}
