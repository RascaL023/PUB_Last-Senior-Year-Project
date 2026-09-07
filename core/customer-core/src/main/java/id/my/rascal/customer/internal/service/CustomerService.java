package id.my.rascal.customer.internal.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.auth.api.AuthApi;
import id.my.rascal.auth.api.CreateAccountRequest;
import id.my.rascal.auth.api.UserAuthApiResponse;
import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.common.exception.ConflictException;
import id.my.rascal.common.exception.NotFoundException;
import id.my.rascal.common.util.StringUtil;
import id.my.rascal.customer.internal.entity.Customer;
import id.my.rascal.customer.internal.model.mapper.CustomerMapper;
import id.my.rascal.customer.internal.model.request.CustomerClaimRequest;
import id.my.rascal.customer.internal.model.request.CustomerPatchRequest;
import id.my.rascal.customer.internal.model.request.CustomerPutRequest;
import id.my.rascal.customer.internal.model.request.CustomerRegisterRequest;
import id.my.rascal.customer.internal.model.request.CustomerRequest;
import id.my.rascal.customer.internal.model.response.CustomerResponse;
import id.my.rascal.customer.internal.repository.CustomerRepository;

@Service
public class CustomerService {

    private static final String CUSTOMER_ROLE = "CUSTOMER_BASE";
    private final CustomerRepository customerRepository;
    private final AuthApi authApi;

    public CustomerService(CustomerRepository customerRepository, AuthApi authApi) {
        this.customerRepository = customerRepository;
        this.authApi = authApi;
    }

    @Transactional
    public CustomerResponse create(CustomerRequest request) {
        Customer customer = new Customer();
        customer.setName(requireName(request.name()));
        customer.setEmail(normalizeNullable(request.email()));
        customer.setPhone(normalizePhone(request.phone()));
        customer.setNotes(normalizeNullable(request.notes()));
        customer.setCreatedAt(LocalDateTime.now());

        return CustomerMapper.toResponse(customerRepository.save(customer));
    }

    @Transactional
    public CustomerResponse register(CustomerRegisterRequest request) {
        UserAuthApiResponse account = authApi.createAccount(
            new CreateAccountRequest(request.email(), request.password(), CUSTOMER_ROLE)
        );

        Customer customer = new Customer();
        customer.setUserAuthId(account.id());
        customer.setName(requireName(request.name()));
        customer.setEmail(normalizeNullable(request.email()));
        customer.setPhone(normalizePhone(request.phone()));
        customer.setCreatedAt(LocalDateTime.now());

        return CustomerMapper.toResponse(customerRepository.save(customer));
    }

    @Transactional
    public CustomerResponse claim(Long id, CustomerClaimRequest request) {
        Customer customer = findActive(id);
        if (customer.getUserAuthId() != null)
            throw new ConflictException("Customer already linked to an account");

        UserAuthApiResponse account = authApi.createAccount(
            new CreateAccountRequest(request.email(), request.password(), CUSTOMER_ROLE)
        );

        customer.setUserAuthId(account.id());
        if (StringUtil.safeIsBlank(customer.getEmail()))
            customer.setEmail(normalizeNullable(request.email()));
        customer.setUpdatedAt(LocalDateTime.now());

        return CustomerMapper.toResponse(customerRepository.save(customer));
    }

    @Transactional
    public CustomerResponse update(Long id, CustomerPutRequest request) {
        Customer customer = findActive(id);

        customer.setName(requireName(request.name()));
        customer.setEmail(normalizeNullable(request.email()));
        customer.setPhone(normalizePhone(request.phone()));
        customer.setNotes(normalizeNullable(request.notes()));
        customer.setUpdatedAt(LocalDateTime.now());

        return CustomerMapper.toResponse(customerRepository.save(customer));
    }

    @Transactional
    public CustomerResponse patch(Long id, CustomerPatchRequest request) {
        Customer customer = findActive(id);

        request.nameOpt().ifPresent(name -> customer.setName(requireName(name)));
        request.emailOpt().ifPresent(email -> customer.setEmail(normalizeNullable(email)));
        request.phoneOpt().ifPresent(phone -> customer.setPhone(normalizePhone(phone)));
        request.notesOpt().ifPresent(notes -> customer.setNotes(normalizeNullable(notes)));
        customer.setUpdatedAt(LocalDateTime.now());

        return CustomerMapper.toResponse(customerRepository.save(customer));
    }

    @Transactional
    public void delete(Long id) {
        Customer customer = findActive(id);
        customer.setDeletedAt(LocalDateTime.now());
        customerRepository.save(customer);
    }


    private Customer findActive(Long id) {
        if (id == null || id <= 0) throw new BadRequestException("Invalid customer ID");
        return customerRepository.findActiveById(id)
            .orElseThrow(() -> new NotFoundException("Customer not found with id: " + id));
    }

    private String requireName(String name) {
        if (StringUtil.safeIsBlank(name) || name.trim().length() < 3)
            throw new BadRequestException("Name must be 3-50 characters");
        return StringUtil.normalizeSpaces(name);
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
