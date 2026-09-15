package id.my.rascal.customer.internal.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.common.util.StringUtil;
import id.my.rascal.customer.internal.entity.Customer;
import id.my.rascal.customer.internal.model.mapper.CustomerMapper;
import id.my.rascal.customer.internal.model.request.CustomerPatchRequest;
import id.my.rascal.customer.internal.model.request.CustomerPutRequest;
import id.my.rascal.customer.internal.model.request.CustomerRequest;
import id.my.rascal.customer.internal.model.response.CustomerResponse;
import id.my.rascal.customer.internal.repository.CustomerRepository;
import id.my.rascal.customer.internal.service.CustomerQueryService;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerQueryService customerQueryService;

    public CustomerService(CustomerRepository customerRepository, CustomerQueryService customerQueryService) {
        this.customerRepository = customerRepository;
        this.customerQueryService = customerQueryService;
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
    public CustomerResponse update(Long id, CustomerPutRequest request) {
        Customer customer = customerQueryService.findById(id);

        customer.setName(requireName(request.name()));
        customer.setEmail(normalizeNullable(request.email()));
        customer.setPhone(normalizePhone(request.phone()));
        customer.setNotes(normalizeNullable(request.notes()));
        customer.setUpdatedAt(LocalDateTime.now());

        return CustomerMapper.toResponse(customerRepository.save(customer));
    }

    @Transactional
    public CustomerResponse patch(Long id, CustomerPatchRequest request) {
        Customer customer = customerQueryService.findById(id);

        request.nameOpt().ifPresent(name -> customer.setName(requireName(name)));
        request.emailOpt().ifPresent(email -> customer.setEmail(normalizeNullable(email)));
        request.phoneOpt().ifPresent(phone -> customer.setPhone(normalizePhone(phone)));
        request.notesOpt().ifPresent(notes -> customer.setNotes(normalizeNullable(notes)));
        customer.setUpdatedAt(LocalDateTime.now());

        return CustomerMapper.toResponse(customerRepository.save(customer));
    }

    @Transactional
    public void delete(Long id) {
        Customer customer = customerQueryService.findById(id);
        customer.setDeletedAt(LocalDateTime.now());
        customerRepository.save(customer);
    }

    @Transactional(readOnly = true)
    public CustomerResponse getById(Long id) {
        return CustomerMapper.toResponse(customerQueryService.findById(id));
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

    private String normalizeNullable(String value) {
        if (StringUtil.safeIsBlank(value)) return null;
        return StringUtil.normalizeSpaces(value);
    }

    private String normalizePhone(String phone) {
        if (StringUtil.safeIsBlank(phone)) return null;
        return StringUtil.normalizeSpaces(phone);
    }
}
