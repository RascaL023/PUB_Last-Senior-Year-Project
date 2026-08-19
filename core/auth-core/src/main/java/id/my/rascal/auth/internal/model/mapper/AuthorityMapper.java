package id.my.rascal.auth.internal.model.mapper;

import id.my.rascal.auth.internal.entity.Authority;
import id.my.rascal.auth.internal.model.response.AuthorityResponse;

public class AuthorityMapper {

    private AuthorityMapper() { }

    public static AuthorityResponse toResponse(Authority authority) {
        if (authority == null) return null;

        return new AuthorityResponse(
            authority.getId(),
            authority.getName(),
            authority.getCreatedAt(),
            authority.getUpdatedAt()
        );
    }

}
