package id.my.rascal.payment.internal.adapter;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.common.exception.NotFoundException;
import id.my.rascal.payment.api.PaymentApi;
import id.my.rascal.payment.api.PaymentApiWebhookRequest;
import id.my.rascal.payment.internal.component.PaymentEffect;
import id.my.rascal.payment.internal.component.PaymentStatusFlowPolicy;
import id.my.rascal.payment.internal.entity.Payment;
import id.my.rascal.payment.internal.model.enums.PaymentStatus;
import id.my.rascal.payment.internal.model.mapper.PaymentMapper;
import id.my.rascal.payment.internal.repository.PaymentRepository;
import id.my.rascal.payment.internal.service.PaymentEventPublisherService;

@Component
public class PaymentApiImpl implements PaymentApi {

    private final PaymentRepository paymentRepository;
    private final PaymentStatusFlowPolicy paymentStatusFlowPolicy;
    private final PaymentEffect paymentEffect;
    private final PaymentEventPublisherService paymentEventPublisherService;
    private static final Logger log = LoggerFactory.getLogger(PaymentApiImpl.class);

    public PaymentApiImpl(
        PaymentRepository paymentRepository,
        PaymentEffect paymentEffect,
        PaymentStatusFlowPolicy paymentStatusFlowPolicy,
        PaymentEventPublisherService paymentEventPublisherService
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentEffect = paymentEffect;
        this.paymentStatusFlowPolicy = paymentStatusFlowPolicy;
        this.paymentEventPublisherService = paymentEventPublisherService;
    }

    @Override
    @Transactional
    public void handleWebhookRequest(PaymentApiWebhookRequest payloadRequest, String raw) {
        if (payloadRequest == null || payloadRequest.externalId() == null)
            throw new BadRequestException("Invalid Xendit webhook payloadRequest");

        Payment payment = paymentRepository.findByExternalId(payloadRequest.externalId()).orElse(null);
        if (payment == null) {
            log.warn("Received Xendit webhook for unknown external_id: {}", payloadRequest.externalId());
            return; // acknowledge to stop retries; no side effect
        }

        PaymentStatus paymentPayloadStatus = PaymentMapper.toPaymentStatus(payloadRequest.status());
        PaymentStatus paymentStatus = payment.getStatus();
        if (paymentStatus == paymentPayloadStatus) return; // idempotent redelivery: ack tanpa efek ganda

        boolean lateSettlement = paymentPayloadStatus == PaymentStatus.PAID
            && (paymentStatus == PaymentStatus.EXPIRED || paymentStatus == PaymentStatus.FAILED);

        if (lateSettlement) {
            log.info("LATE_PAYMENT settling previously {} payment: paymentId={} externalId={}",
                paymentStatus, payment.getId(), payloadRequest.externalId());
        } else {
            try {
                paymentStatusFlowPolicy.validateFlow(paymentStatus, paymentPayloadStatus);
            } catch (BadRequestException e) {
                log.warn("Stale/inapplicable webhook, acknowledged: paymentId={} current={} incoming={} externalId={} reason={}",
                    payment.getId(), paymentStatus, paymentPayloadStatus, payloadRequest.externalId(), e.getMessage());
                return; // ack agar Xendit berhenti
            }
        }

        payment.setStatus(paymentPayloadStatus);

        if (payloadRequest.paidAmount() != null) payment.setAmount(payloadRequest.paidAmount());
        else
            log.warn("Webhook tanpa paid_amount, amount dipertahankan: paymentId={} externalId={}",
                payment.getId(), payloadRequest.externalId());
        paymentEffect.applyEffectIfPaid(payment);

        payment.setRawWebhook(raw);
        payment.setPaymentMethodName(payloadRequest.paymentMethod());
        payment.setPaymentChannel(payloadRequest.paymentChannel());
        if (paymentPayloadStatus == PaymentStatus.REFUNDED && payment.getRefundedAt() == null)
            payment.setRefundedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());
        Payment saved = paymentRepository.save(payment);
        if (saved.getStatus() == PaymentStatus.PAID)
            paymentEventPublisherService.publishSettled(saved, saved.getAmount());
        else if (saved.getStatus() == PaymentStatus.REFUNDED)
            paymentEventPublisherService.publishRefunded(saved);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void confirmSplit(Long paymentId, Integer appliedAmount, Integer excessAmount) {
        Payment payment = paymentRepository.findActiveById(paymentId)
            .orElseThrow(() -> new NotFoundException("Payment not found with id: " + paymentId));

        payment.setAppliedAmount(appliedAmount);
        payment.setExcessAmount(excessAmount);
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        log.info("Recorded settlement split: paymentId={} applied={} excess={}",
            paymentId, appliedAmount, excessAmount);
    }

}
