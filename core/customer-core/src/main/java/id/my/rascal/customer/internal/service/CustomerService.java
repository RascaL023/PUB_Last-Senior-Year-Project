package id.my.rascal.customer.internal.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.auth.api.AuthApi;
import id.my.rascal.auth.api.CreateAccountRequest;
import id.my.rascal.auth.api.UserAuthApiResponse;
import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.common.util.StringUtil;
import id.my.rascal.customer.internal.entity.Customer;
import id.my.rascal.customer.internal.model.mapper.CustomerMapper;
import id.my.rascal.customer.internal.model.request.CustomerPatchRequest;
import id.my.rascal.customer.internal.model.request.CustomerPutRequest;
import id.my.rascal.customer.internal.model.request.CustomerRegisterRequest;
import id.my.rascal.customer.internal.model.response.CustomerResponse;
import id.my.rascal.customer.internal.repository.CustomerRepository;

@Service
public class CustomerService {

    private static final String CUSTOMER_ROLE = "CUSTOMER_BASE";

    private final CustomerRepository customerRepository;
    private final CustomerQueryService customerQueryService;
    private final AuthApi authApi;

    public CustomerService(
        CustomerRepository customerRepository, 
        CustomerQueryService customerQueryService, 
        AuthApi authApi
    ) {
        this.customerRepository = customerRepository;
        this.customerQueryService = customerQueryService;
        this.authApi = authApi;
    }

    @Transactional
    public CustomerResponse register(CustomerRegisterRequest request) {
        String email = requireEmail(request.email());

        UserAuthApiResponse auth = authApi.createAccount(
            new CreateAccountRequest(
                email, 
                request.password(), CUSTOMER_ROLE
            )
        );

        Customer customer = new Customer();
        customer.setUserAuthId(auth.id());
        customer.setName(requireName(request.name()));
        customer.setEmail(email);
        customer.setPhone(normalizePhone(request.phone()));
        customer.setCreatedAt(LocalDateTime.now());

        return CustomerMapper.toResponse(customerRepository.save(customer));
    }

    @Transactional
    public CustomerResponse update(Long id, CustomerPutRequest request) {
        Customer customer = customerQueryService.findById(id);

        customer.setName(requireName(request.name()));
        customer.setPhone(normalizePhone(request.phone()));
        customer.setNotes(normalizeNullable(request.notes()));
        customer.setUpdatedAt(LocalDateTime.now());

        return CustomerMapper.toResponse(customerRepository.save(customer));
    }

    @Transactional
    public CustomerResponse patch(Long id, CustomerPatchRequest request) {
        Customer customer = customerQueryService.findById(id);

        request.nameOpt().ifPresent(name -> customer.setName(requireName(name)));
        request.phoneOpt().ifPresent(phone -> customer.setPhone(normalizePhone(phone)));
        request.notesOpt().ifPresent(notes -> customer.setNotes(normalizeNullable(notes)));
        customer.setUpdatedAt(LocalDateTime.now());

        return CustomerMapper.toResponse(customerRepository.save(customer));
    }

    @Transactional
    public void delete(Long id) {
        Customer customer = customerQueryService.findById(id);
        if (customer.getUserAuthId() != null)
            authApi.softDeleteAccount(customer.getUserAuthId());
        customer.setDeletedAt(LocalDateTime.now());
        customerRepository.save(customer);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getById(Long id) {
        return CustomerMapper.toResponse(customerQueryService.findById(id));
    }

    @Transactional(readOnly = true)
    public CustomerResponse getMe(Long userAuthId) {
        return CustomerMapper.toResponse(customerQueryService.findByUserAuthId(userAuthId));
    }

    @Transactional
    public CustomerResponse updateMe(Long userAuthId, CustomerPutRequest request) {
        Customer customer = customerQueryService.findByUserAuthId(userAuthId);

        customer.setName(requireName(request.name()));
        customer.setPhone(normalizePhone(request.phone()));
        customer.setNotes(normalizeNullable(request.notes()));
        customer.setUpdatedAt(LocalDateTime.now());

        return CustomerMapper.toResponse(customerRepository.save(customer));
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> search(String keyword, Pageable pageable) {
        return customerQueryService.search(keyword, pageable).map(CustomerMapper::toResponse);
    }

    private String requireName(String name) {
        if (StringUtil.safeIsBlank(name) || name.trim().length() < 3)
            throw new BadRequestException("Name must be 3-50 characters");
        return StringUtil.normalizeSpaces(name);
    }

    private String requireEmail(String email) {
        if (StringUtil.safeIsBlank(email))
            throw new BadRequestException("Email is required");
        return StringUtil.normalizeSpaces(email).trim().toLowerCase();
    }

    private String normalizeNullable(String value) {
        if (StringUtil.safeIsBlank(value)) return null;
        return StringUtil.normalizeSpaces(value);
    }

    private String normalizePhone(String phone) {
        if (StringUtil.safeIsBlank(phone)) return null;
        return StringUtil.normalizeSpaces(phone);
    }
}
