package id.my.rascal.payment.internal.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.common.exception.NotFoundException;
import id.my.rascal.invoice.api.InvoiceApi;
import id.my.rascal.invoice.api.InvoiceApiResponse;
import id.my.rascal.invoice.api.InvoiceItemApiResponse;
import id.my.rascal.payment.api.PaymentProcessor;
import id.my.rascal.payment.api.PaymentProcessorRequest;
import id.my.rascal.payment.api.PaymentProcessorResponse;
import id.my.rascal.payment.internal.entity.Payment;
import id.my.rascal.payment.internal.component.PaymentEffect;
import id.my.rascal.payment.internal.component.PaymentProcessorResolver;
import id.my.rascal.payment.internal.component.PaymentStatusFlowPolicy;
import id.my.rascal.payment.internal.model.enums.PaymentProvider;
import id.my.rascal.payment.internal.model.enums.PaymentStatus;
import id.my.rascal.payment.internal.model.enums.PaymentTargetType;
import id.my.rascal.payment.internal.model.mapper.PaymentMapper;
import id.my.rascal.payment.internal.model.request.PaymentRefundRequest;
import id.my.rascal.payment.internal.model.request.PaymentRequest;
import id.my.rascal.payment.internal.model.response.PaymentResponse;
import id.my.rascal.payment.internal.repository.PaymentRepository;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private final PaymentRepository paymentRepository;
    private final PaymentStatusFlowPolicy paymentStatusFlowPolicy;
    private final PaymentProcessorResolver paymentProcessorResolver;
    private final PaymentEffect paymentEffect;
    private final InvoiceApi invoiceApi;
    private final PaymentEventPublisherService paymentEventPublisherService;
    private final TransactionTemplate transactionTemplate;

    public PaymentService(
        PaymentRepository paymentRepository,
        PaymentStatusFlowPolicy paymentStatusFlowPolicy,
        PaymentProcessorResolver paymentProcessorResolver,
        InvoiceApi invoiceApi,
        PaymentEventPublisherService paymentEventPublisherService,
        PaymentEffect paymentEffect,
        TransactionTemplate transactionTemplate
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentStatusFlowPolicy = paymentStatusFlowPolicy;
        this.paymentProcessorResolver = paymentProcessorResolver;
        this.invoiceApi = invoiceApi;
        this.paymentEventPublisherService = paymentEventPublisherService;
        this.paymentEffect = paymentEffect;
        this.transactionTemplate = transactionTemplate;
    }

    public PaymentResponse create(PaymentRequest request) {
        if (request.targetType() != PaymentTargetType.INVOICE)
            throw new BadRequestException(
                "Unsupported payment target: " + request.targetType() + 
                ". Payment hanya dapat menarget INVOICE"
            );

        ResolvedTarget target = resolveInvoice(request.targetId());
        if (target.amount() == null || target.amount() <= 0) throw new BadRequestException("Invoice already paid");
        String externalId = "INV-" + UUID.randomUUID();

        PaymentProcessor processor = paymentProcessorResolver.resolve(request.paymentProvider().toString());
        PaymentProcessorResponse processorResponse;
        try {
             processorResponse = processor.process(
                new PaymentProcessorRequest(
                    target.amount(),
                    "IDR",
                    target.reference(),
                    externalId,
                    null, null
                )
            );
        } catch (Exception e) {
            // Mark failed?
            log.error(e.getMessage());
            throw new BadRequestException(e.getMessage());
        }

        return transactionTemplate.execute(status ->
            persistCreatedPayment(request, target, externalId, processor, processorResponse)
        );
    }

    public PaymentResponse persistCreatedPayment(
        PaymentRequest request,
        ResolvedTarget target,
        String externalId,
        PaymentProcessor processor,
        PaymentProcessorResponse processorResponse
    ) {
        // Berjalan di dalam transaksi dari TransactionTemplate.
        Payment payment = new Payment();
        payment.setPaymentProvider(PaymentProvider.valueOf(processor.paymentProvider()));
        payment.setPaymentMethodName(processorResponse.paymentMethodName());
        payment.setPaymentChannel(processorResponse.paymentChannel());
        payment.setTargetType(request.targetType());
        payment.setTargetId(request.targetId());
        payment.setTargetReference(target.reference());
        payment.setAmount(target.amount());
        payment.setPaymentDetail(request.paymentDetail());
        payment.setExternalId(externalId);

        payment.setStatus(PaymentMapper.toPaymentStatus(processorResponse.status()));
        paymentEffect.applyEffectIfPaid(payment);

        payment.setInvoiceUrl(processorResponse.invoiceUrl());
        payment.setCreatedAt(LocalDateTime.now());

        Payment saved = paymentRepository.save(payment);
        if (saved.getStatus() == PaymentStatus.PAID)
            paymentEventPublisherService.publishSettled(saved, saved.getAmount());

        return toResponse(saved);
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
        PaymentProvider paymentProvider,
        Pageable pageable
    ) {
        return paymentRepository
            .searchActive(keyword, targetType, targetId, status, paymentProvider, pageable)
            .map(this::toResponse);
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
        return markRefunded(id, null);
    }

    @Transactional
    public PaymentResponse markRefunded(Long id, PaymentRefundRequest request) {
        Payment payment = findActive(id);
        PaymentStatus target = PaymentStatus.REFUNDED;

        paymentStatusFlowPolicy.validateFlow(payment.getStatus(), target);
        LocalDateTime now = LocalDateTime.now();
        payment.setStatus(target);
        payment.setRefundedAt(now);
        payment.setUpdatedAt(now);
        Payment saved = paymentRepository.save(payment);
        paymentEventPublisherService.publishRefunded(saved);

        if (saved.getTargetType() == PaymentTargetType.INVOICE) {
            List<Long> orderItemIds = resolveRefundOrderItemIds(saved.getTargetId(), request);
            if (!orderItemIds.isEmpty())
                invoiceApi.refundItems(saved.getTargetId(), orderItemIds, saved.getId());
        }
        return toResponse(saved);
    }

    private List<Long> resolveRefundOrderItemIds(
        Long invoiceId,
        PaymentRefundRequest request
    ) {
        if (request != null && request.orderItemIds() != null && !request.orderItemIds().isEmpty())
            return request.orderItemIds();

        InvoiceApiResponse invoice = invoiceApi.getInvoice(invoiceId);
        return invoice.items().stream()
            .filter(i -> !Boolean.TRUE.equals(i.refunded()))
            .map(InvoiceItemApiResponse::orderItemId)
            .toList();
    }


    private PaymentResponse transition(Long id, PaymentStatus target) {
        Payment payment = findActive(id);
        paymentStatusFlowPolicy.validateFlow(payment.getStatus(), target);
        payment.setStatus(target);
        payment.setUpdatedAt(LocalDateTime.now());
        return toResponse(paymentRepository.save(payment));
    }

    private ResolvedTarget resolveInvoice(Long targetId) {
        InvoiceApiResponse invoice = invoiceApi.getInvoice(targetId);
        return new ResolvedTarget(invoice.remainingAmount(), invoice.invoiceNumber());
    }

    private Payment findActive(Long id) {
        return paymentRepository.findActiveById(id)
            .orElseThrow(() -> new NotFoundException("Payment not found with id: " + id));
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
            payment.getId(),
            payment.getTargetType(),
            payment.getTargetId(),
            payment.getTargetReference(),
            payment.getPaymentProvider(),
            payment.getPaymentMethodName(),
            payment.getExternalId(),
            payment.getInvoiceUrl(),
            payment.getStatus(),
            payment.getPaymentChannel(),
            payment.getPaymentDetail(),
            payment.getAmount(),
            payment.getAppliedAmount(),
            payment.getExcessAmount(),
            payment.getPaidAt(),
            payment.getCreatedAt(),
            payment.getUpdatedAt()
        );
    }    private record ResolvedTarget(Integer amount, String reference) {}

}
