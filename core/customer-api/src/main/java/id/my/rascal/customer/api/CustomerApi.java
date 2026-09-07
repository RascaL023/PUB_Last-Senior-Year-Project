package id.my.rascal.customer.api;

import java.util.Optional;

public interface CustomerApi {

    boolean existsById(Long id);
    Optional<CustomerApiResponse> getById(Long id);
    Optional<CustomerApiResponse> getByUserAuthId(Long userAuthId);

}
