package id.my.rascal.customer.internal.model.mapper;

import id.my.rascal.customer.internal.entity.Customer;
import id.my.rascal.customer.internal.model.response.CustomerResponse;

public final class CustomerMapper {

    private CustomerMapper() {}

    public static CustomerResponse toResponse(Customer customer) {
        return new CustomerResponse(
            customer.getId(),
            customer.getUserAuthId(),
            customer.getName(),
            customer.getEmail(),
            customer.getPhone(),
            customer.getNotes(),
            customer.getCreatedAt(),
            customer.getUpdatedAt()
        );
    }

}
