package id.my.rascal.customer.internal.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.common.exception.NotFoundException;
import id.my.rascal.common.util.StringUtil;
import id.my.rascal.customer.internal.entity.Customer;
import id.my.rascal.customer.internal.repository.CustomerRepository;

@Service
public class CustomerQueryService {

    private final CustomerRepository customerRepository;

    public CustomerQueryService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Transactional(readOnly = true)
    public Customer findById(Long id) {
        if (id == null || id <= 0)
            throw new BadRequestException("Invalid customer ID");
        return customerRepository.findActiveById(id)
            .orElseThrow(() -> new NotFoundException("Customer not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public Customer findByUserAuthId(Long userAuthId) {
        if (userAuthId == null || userAuthId <= 0)
            throw new BadRequestException("Invalid userAuthId");
        return customerRepository.findActiveByUserAuthId(userAuthId)
            .orElseThrow(() -> new NotFoundException("Customer not found with userAuthId: " + userAuthId));
    }

    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        if (id == null || id <= 0)
            return false;
        return customerRepository.existsActiveById(id);
    }

    @Transactional(readOnly = true)
    public Page<Customer> search(String keyword, Pageable pageable) {
        Page<Long> idPage = customerRepository.findSearchIds(normalize(keyword), pageable);
        if (idPage.getContent().isEmpty()) return Page.empty(pageable);

        List<Customer> customers = customerRepository.findAllActiveByIds(idPage.getContent());
        Map<Long, Customer> byId = customers.stream()
            .collect(Collectors.toMap(Customer::getId, Function.identity()));

        List<Customer> responses = new ArrayList<>();
        for (Long id : idPage.getContent()) {
            Customer customer = byId.get(id);
            if (customer != null) responses.add(customer);
        }

        return new PageImpl<>(responses, pageable, idPage.getTotalElements());
    }

    private String normalize(String keyword) {
        if (StringUtil.safeIsBlank(keyword))
            return "";
        return StringUtil.normalizeSearch(keyword);
    }
}
