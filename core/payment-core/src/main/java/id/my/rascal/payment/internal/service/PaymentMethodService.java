package id.my.rascal.payment.internal.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.common.exception.ConflictException;
import id.my.rascal.common.exception.NotFoundException;
import id.my.rascal.common.util.StringUtil;
import id.my.rascal.payment.internal.entity.PaymentMethod;
import id.my.rascal.payment.internal.model.request.PaymentMethodPatchRequest;
import id.my.rascal.payment.internal.model.request.PaymentMethodPutRequest;
import id.my.rascal.payment.internal.model.request.PaymentMethodRequest;
import id.my.rascal.payment.internal.model.response.PaymentMethodResponse;
import id.my.rascal.payment.internal.repository.PaymentMethodRepository;

@Service
public class PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;

    public PaymentMethodService(PaymentMethodRepository paymentMethodRepository) {
        this.paymentMethodRepository = paymentMethodRepository;
    }

    @Transactional
    public PaymentMethodResponse create(PaymentMethodRequest request) {
        if (paymentMethodRepository.existsByCode(request.code().toUpperCase()))
            throw new ConflictException("Payment method code already exists: " + request.code());
        if (paymentMethodRepository.existsByName(StringUtil.normalizeSpaces(request.name())))
            throw new ConflictException("Payment method name already exists: " + request.name());

        PaymentMethod method = new PaymentMethod();
        method.setCode(request.code().toUpperCase());
        method.setName(StringUtil.normalizeSpaces(request.name()));
        method.setIsActive(request.isActive() == null || request.isActive());
        method.setCreatedAt(LocalDateTime.now());

        return toResponse(paymentMethodRepository.save(method));
    }

    @Transactional(readOnly = true)
    public PaymentMethodResponse getById(Long id) {
        return toResponse(findActive(id));
    }

    @Transactional(readOnly = true)
    public Page<PaymentMethodResponse> search(String keyword, Pageable pageable) {
        return paymentMethodRepository
            .searchActive(StringUtil.normalizeSearch(keyword), pageable)
            .map(this::toResponse);
    }

    @Transactional
    public PaymentMethodResponse update(Long id, PaymentMethodPutRequest request) {
        PaymentMethod method = findActive(id);
        ensureEditable(method);

        String code = request.code().toUpperCase();
        String name = StringUtil.normalizeSpaces(request.name());
        if (!method.getCode().equals(code) && paymentMethodRepository.existsByCode(code))
            throw new ConflictException("Payment method code already exists: " + code);
        if (!method.getName().equals(name) && paymentMethodRepository.existsByName(name))
            throw new ConflictException("Payment method name already exists: " + name);

        method.setCode(code);
        method.setName(name);
        applyActive(method, request.isActive());
        method.setUpdatedAt(LocalDateTime.now());

        return toResponse(paymentMethodRepository.save(method));
    }

    @Transactional
    public PaymentMethodResponse patch(Long id, PaymentMethodPatchRequest request) {
        PaymentMethod method = findActive(id);
        if (request.isEmptyPatch()) throw new BadRequestException("PATCH can't be empty");

        if (request.code().isPresent()) {
            ensureEditable(method);
            String code = request.code().get().toUpperCase();
            if (!method.getCode().equals(code) && paymentMethodRepository.existsByCode(code))
                throw new ConflictException("Payment method code already exists: " + code);
            method.setCode(code);
        }

        if (request.name().isPresent()) {
            ensureEditable(method);
            String name = StringUtil.normalizeSpaces(request.name().get());
            if (!method.getName().equals(name) && paymentMethodRepository.existsByName(name))
                throw new ConflictException("Payment method name already exists: " + name);
            method.setName(name);
        }

        if (request.isActive().isPresent()) {
            applyActive(method, request.isActive().get());
        }

        method.setUpdatedAt(LocalDateTime.now());
        return toResponse(paymentMethodRepository.save(method));
    }

    @Transactional
    public void delete(Long id) {
        PaymentMethod method = findActive(id);
        method.setDeletedAt(LocalDateTime.now());
        paymentMethodRepository.save(method);
    }

    public PaymentMethod findActive(Long id) {
        return paymentMethodRepository.findActiveById(id)
            .orElseThrow(() -> new NotFoundException("Payment method not found with id: " + id));
    }

    private void ensureEditable(PaymentMethod method) {
        // soft-deleted guard not needed; placeholder for future invariants
    }

    private void applyActive(PaymentMethod method, Boolean isActive) {
        method.setIsActive(isActive == null ? Boolean.TRUE : isActive);
    }

    private PaymentMethodResponse toResponse(PaymentMethod method) {
        return new PaymentMethodResponse(
            method.getId(),
            method.getCode(),
            method.getName(),
            method.getIsActive(),
            method.getCreatedAt(),
            method.getUpdatedAt()
        );
    }
}
