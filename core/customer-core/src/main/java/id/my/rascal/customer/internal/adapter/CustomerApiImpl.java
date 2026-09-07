package id.my.rascal.customer.internal.adapter;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.customer.api.CustomerApi;
import id.my.rascal.customer.api.CustomerApiResponse;
import id.my.rascal.customer.internal.repository.CustomerRepository;

@Component
public class CustomerApiImpl implements CustomerApi {

    private final CustomerRepository customerRepository;

    public CustomerApiImpl(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        if (id == null || id <= 0)
            return false;
        return customerRepository.existsActiveById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CustomerApiResponse> getById(Long id) {
        if (id == null || id <= 0)
            return Optional.empty();
        return customerRepository.findActiveById(id)
            .map(c -> CustomerApiResponse.of(c.getId(), c.getUserAuthId(), c.getName(), c.getEmail(), c.getPhone()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CustomerApiResponse> getByUserAuthId(Long userAuthId) {
        if (userAuthId == null || userAuthId <= 0)
            return Optional.empty();
        return customerRepository.findActiveByUserAuthId(userAuthId)
            .map(c -> CustomerApiResponse.of(c.getId(), c.getUserAuthId(), c.getName(), c.getEmail(), c.getPhone()));
    }

}
