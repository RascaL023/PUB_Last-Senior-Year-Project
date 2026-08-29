package id.my.rascal.payment.internal.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.common.exception.NotFoundException;
import id.my.rascal.dining.api.DiningApi;
import id.my.rascal.dining.api.DiningApiResponse;
import id.my.rascal.order.api.OrderApi;
import id.my.rascal.order.api.OrderApiResponse;
import id.my.rascal.payment.internal.entity.Payment;
import id.my.rascal.payment.internal.entity.PaymentMethod;
import id.my.rascal.payment.internal.model.enums.PaymentStatus;
import id.my.rascal.payment.internal.model.enums.PaymentTargetType;
import id.my.rascal.payment.internal.model.request.PaymentPatchRequest;
import id.my.rascal.payment.internal.model.request.PaymentPutRequest;
import id.my.rascal.payment.internal.model.request.PaymentRequest;
import id.my.rascal.payment.internal.model.response.PaymentResponse;
import id.my.rascal.payment.internal.repository.PaymentRepository;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMethodService paymentMethodService;
    private final PaymentStatusFlowPolicy paymentStatusFlowPolicy;
    private final OrderApi orderApi;
    private final DiningApi diningApi;

    public PaymentService(
        PaymentRepository paymentRepository,
        PaymentMethodService paymentMethodService,
        PaymentStatusFlowPolicy paymentStatusFlowPolicy,
        OrderApi orderApi,
        DiningApi diningApi
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentMethodService = paymentMethodService;
        this.paymentStatusFlowPolicy = paymentStatusFlowPolicy;
        this.orderApi = orderApi;
        this.diningApi = diningApi;
    }

    @Transactional
    public PaymentResponse create(PaymentRequest request) {
        ResolvedTarget target = resolveTarget(request.targetType(), request.targetId());
        PaymentMethod method = paymentMethodService.findActive(request.paymentMethodId());

        Payment payment = new Payment();
        payment.setTargetType(request.targetType());
        payment.setTargetId(request.targetId());
        payment.setTargetReference(target.reference());
        payment.setPaymentMethodId(method.getId());
        payment.setPaymentMethodName(method.getName());
        payment.setAmount(target.amount());
        payment.setPaymentChannel(request.paymentChannel());
        payment.setPaymentDetail(request.paymentDetail());
        payment.setExternalId(request.externalId());
        payment.setInvoiceUrl(request.invoiceUrl());
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(LocalDateTime.now());

        return toResponse(paymentRepository.save(payment));
    }

    @Transactional(readOnly = true)
    public PaymentResponse getById(Long id) {
        return toResponse(findActive(id));
    }

    @Transactional(readOnly = true)
    public Page<PaymentResponse> search(
        String keyword,
        PaymentTargetType targetType,
        Long targetId,
        PaymentStatus status,
        Long paymentMethodId,
        Pageable pageable
    ) {
        return paymentRepository
            .searchActive(keyword, targetType, targetId, status, paymentMethodId, pageable)
            .map(this::toResponse);
    }

    @Transactional
    public PaymentResponse update(Long id, PaymentPutRequest request) {
        Payment payment = findActive(id);
        ensureMutable(payment);

        ResolvedTarget target = resolveTarget(request.targetType(), request.targetId());
        PaymentMethod method = paymentMethodService.findActive(request.paymentMethodId());

        payment.setTargetType(request.targetType());
        payment.setTargetId(request.targetId());
        payment.setTargetReference(target.reference());
        payment.setPaymentMethodId(method.getId());
        payment.setPaymentMethodName(method.getName());
        payment.setAmount(target.amount());
        payment.setPaymentChannel(request.paymentChannel());
        payment.setPaymentDetail(request.paymentDetail());
        payment.setExternalId(request.externalId());
        payment.setInvoiceUrl(request.invoiceUrl());
        payment.setUpdatedAt(LocalDateTime.now());

        return toResponse(paymentRepository.save(payment));
    }

    @Transactional
    public PaymentResponse patch(Long id, PaymentPatchRequest request) {
        Payment payment = findActive(id);
        if (request.isEmptyPatch()) throw new BadRequestException("PATCH can't be empty");
        ensureMutable(payment);

        if (request.targetType().isPresent() || request.targetId().isPresent()) {
            if (request.targetType().isEmpty() || request.targetId().isEmpty()) {
                throw new BadRequestException("targetType and targetId must be provided together");
            }
            ResolvedTarget target = resolveTarget(request.targetType().get(), request.targetId().get());
            payment.setTargetType(request.targetType().get());
            payment.setTargetId(request.targetId().get());
            payment.setTargetReference(target.reference());
            payment.setAmount(target.amount());
        }

        if (request.paymentMethodId().isPresent()) {
            PaymentMethod method = paymentMethodService.findActive(request.paymentMethodId().get());
            payment.setPaymentMethodId(method.getId());
            payment.setPaymentMethodName(method.getName());
        }

        request.paymentChannel().ifPresent(payment::setPaymentChannel);
        request.paymentDetail().ifPresent(payment::setPaymentDetail);
        request.externalId().ifPresent(payment::setExternalId);
        request.invoiceUrl().ifPresent(payment::setInvoiceUrl);

        payment.setUpdatedAt(LocalDateTime.now());
        return toResponse(paymentRepository.save(payment));
    }

    @Transactional
    public void delete(Long id) {
        Payment payment = findActive(id);
        payment.setDeletedAt(LocalDateTime.now());
        paymentRepository.save(payment);
    }

    @Transactional
    public PaymentResponse markPaid(Long id) {
        Payment payment = findActive(id);
        paymentStatusFlowPolicy.validateFlow(payment.getStatus(), PaymentStatus.PAID);

        switch (payment.getTargetType()) {
            case ORDER -> orderApi.markPaid(payment.getTargetId());
            case DINE_IN -> {
                List<Long> orderIds = diningApi.getOrderIds(payment.getTargetId());
                if (orderIds.isEmpty()) 
                    throw new BadRequestException("There is no orders on " + payment.getTargetType() + " id " + payment.getTargetId());
                orderApi.markPaid(orderIds);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        payment.setPaidAt(now);
        payment.setUpdatedAt(now);
        payment.setStatus(PaymentStatus.PAID);

        return toResponse(paymentRepository.save(payment));
    }

    @Transactional
    public PaymentResponse markExpired(Long id) {
        return transition(id, PaymentStatus.EXPIRED);
    }

    @Transactional
    public PaymentResponse markFailed(Long id) {
        return transition(id, PaymentStatus.FAILED);
    }

    @Transactional
    public PaymentResponse markRefunded(Long id) {
        Payment payment = findActive(id);
        paymentStatusFlowPolicy.validateFlow(payment.getStatus(), PaymentStatus.REFUNDED);
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setUpdatedAt(LocalDateTime.now());
        return toResponse(paymentRepository.save(payment));
    }

    private PaymentResponse transition(Long id, PaymentStatus target) {
        Payment payment = findActive(id);
        paymentStatusFlowPolicy.validateFlow(payment.getStatus(), target);
        payment.setStatus(target);
        payment.setUpdatedAt(LocalDateTime.now());
        return toResponse(paymentRepository.save(payment));
    }

    private ResolvedTarget resolveTarget(PaymentTargetType type, Long targetId) {
        return switch (type) {
            case ORDER -> resolveOrder(targetId);
            case DINE_IN -> resolveDining(targetId);
        };
    }

    private ResolvedTarget resolveOrder(Long targetId) {
        OrderApiResponse order = orderApi.getOrder(targetId);
        return new ResolvedTarget(order.totalPrice(), order.orderNumber());
    }

    private ResolvedTarget resolveDining(Long targetId) {
        DiningApiResponse dining = diningApi.getDining(targetId);
        return new ResolvedTarget(dining.totalPrice(), "DINING-" + dining.id());
    }

    private Payment findActive(Long id) {
        return paymentRepository.findActiveById(id)
            .orElseThrow(() -> new NotFoundException("Payment not found with id: " + id));
    }

    private void ensureMutable(Payment payment) {
        if (payment.getStatus() == PaymentStatus.PAID
            || payment.getStatus() == PaymentStatus.REFUNDED
            || payment.getStatus() == PaymentStatus.EXPIRED
            || payment.getStatus() == PaymentStatus.FAILED) {
            throw new BadRequestException("Cannot modify a " + payment.getStatus() + " payment");
        }
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
            payment.getId(),
            payment.getTargetType(),
            payment.getTargetId(),
            payment.getTargetReference(),
            payment.getPaymentMethodId(),
            payment.getPaymentMethodName(),
            payment.getExternalId(),
            payment.getInvoiceUrl(),
            payment.getStatus(),
            payment.getPaymentChannel(),
            payment.getPaymentDetail(),
            payment.getAmount(),
            payment.getPaidAt(),
            payment.getCreatedAt(),
            payment.getUpdatedAt()
        );
    }

    private record ResolvedTarget(Integer amount, String reference) {}
}
