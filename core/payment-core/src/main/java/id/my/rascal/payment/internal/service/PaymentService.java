package id.my.rascal.payment.internal.service;

import java.time.LocalDateTime;
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
import id.my.rascal.payment.api.PaymentProcessor;
import id.my.rascal.payment.api.PaymentProcessorRequest;
import id.my.rascal.payment.api.PaymentProcessorResponse;
import id.my.rascal.payment.internal.entity.Payment;
import id.my.rascal.payment.internal.component.PaymentEffect;
import id.my.rascal.payment.internal.component.PaymentProcessorResolver;
import id.my.rascal.payment.internal.component.PaymentStatusFlowPolicy;
import id.my.rascal.payment.internal.model.enums.PaymentProvider;
import id.my.rascal.payment.internal.model.enums.PaymentStatus;
import id.my.rascal.payment.internal.model.mapper.PaymentMapper;
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
        ResolvedTarget target = resolveInvoice(request.invoiceId());
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
        payment.setInvoiceId(request.invoiceId());
        payment.setInvoiceNumber(target.reference());
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
        Long invoiceId,
        PaymentStatus status,
        PaymentProvider paymentProvider,
        Pageable pageable
    ) {
        return paymentRepository
            .searchActive(keyword, invoiceId, status, paymentProvider, pageable)
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

    private PaymentResponse transition(Long id, PaymentStatus target) {
        Payment payment = findActive(id);
        paymentStatusFlowPolicy.validateFlow(payment.getStatus(), target);
        payment.setStatus(target);
        payment.setUpdatedAt(LocalDateTime.now());
        return toResponse(paymentRepository.save(payment));
    }

    private ResolvedTarget resolveInvoice(Long invoiceId) {
        InvoiceApiResponse invoice = invoiceApi.getInvoice(invoiceId);
        return new ResolvedTarget(invoice.remainingAmount(), invoice.invoiceNumber());
    }

    private Payment findActive(Long id) {
        return paymentRepository.findActiveById(id)
            .orElseThrow(() -> new NotFoundException("Payment not found with id: " + id));
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
            payment.getId(),
            payment.getInvoiceId(),
            payment.getInvoiceNumber(),
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
