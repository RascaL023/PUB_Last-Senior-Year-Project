package id.my.rascal.customer.api;

import java.util.Optional;

public record CustomerApiResponse(
    Long id,
    Long userAuthId,
    String name,
    String email,
    String phone
) {
    public static CustomerApiResponse of(
        Long id, Long userAuthId, 
        String name, String email, String phone
    ) {
        return new CustomerApiResponse(
            id, userAuthId, 
            name, email, phone
        );
    }

    public Optional<Long> userAuthIdOpt() {
        return Optional.ofNullable(userAuthId);
    }

}
