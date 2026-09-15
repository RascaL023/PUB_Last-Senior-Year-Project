package id.my.rascal.customer.internal.adapter;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.common.exception.NotFoundException;
import id.my.rascal.customer.api.CustomerApi;
import id.my.rascal.customer.api.CustomerApiResponse;
import id.my.rascal.customer.internal.model.mapper.CustomerMapper;
import id.my.rascal.customer.internal.service.CustomerQueryService;

@Component
public class CustomerApiImpl implements CustomerApi {

    private final CustomerQueryService customerQueryService;

    public CustomerApiImpl(CustomerQueryService customerQueryService) {
        this.customerQueryService = customerQueryService;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return customerQueryService.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CustomerApiResponse> getById(Long id) {
        try {
            return Optional.of(CustomerMapper.toApiResponse(customerQueryService.findById(id)));
        } catch (BadRequestException | NotFoundException e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CustomerApiResponse> getByUserAuthId(Long userAuthId) {
        try {
            return Optional.of(CustomerMapper.toApiResponse(customerQueryService.findByUserAuthId(userAuthId)));
        } catch (BadRequestException | NotFoundException e) {
            return Optional.empty();
        }
    }

}
