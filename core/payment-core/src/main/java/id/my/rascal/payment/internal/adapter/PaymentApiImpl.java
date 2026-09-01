package id.my.rascal.payment.internal.adapter;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.payment.api.PaymentApi;
import id.my.rascal.payment.api.PaymentApiWebhookRequest;
import id.my.rascal.payment.internal.entity.Payment;
import id.my.rascal.payment.internal.model.enums.PaymentStatus;
import id.my.rascal.payment.internal.model.mapper.PaymentMapper;
import id.my.rascal.payment.internal.repository.PaymentRepository;
import id.my.rascal.payment.internal.service.PaymentEffect;
import id.my.rascal.payment.internal.service.PaymentStatusFlowPolicy;

@Component
public class PaymentApiImpl implements PaymentApi {

    private final PaymentRepository paymentRepository;
    private final PaymentStatusFlowPolicy paymentStatusFlowPolicy;
    private final PaymentEffect paymentEffect;
    private static final Logger log = LoggerFactory.getLogger(PaymentApiImpl.class);

    public PaymentApiImpl(
        PaymentRepository paymentRepository,
        PaymentEffect paymentEffect,
        PaymentStatusFlowPolicy paymentStatusFlowPolicy
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentEffect = paymentEffect;
        this.paymentStatusFlowPolicy = paymentStatusFlowPolicy;
    }

    @Override
    public void handleWeebhookRequest(PaymentApiWebhookRequest payloadRequest, String raw) {
        if (payloadRequest == null || payloadRequest.externalId() == null)
            throw new BadRequestException("Invalid Xendit webhook payloadRequest");

        Payment payment = paymentRepository.findByExternalId(payloadRequest.externalId()).orElse(null);
        if (payment == null) {
            log.warn("Received Xendit webhook for unknown external_id: {}", payloadRequest.externalId());
            return; // acknowledge to stop retries; no side effect
        }

        PaymentStatus paymentStatus = PaymentMapper.toPaymentStatus(payloadRequest.status());
        paymentStatusFlowPolicy.validateFlow(payment.getStatus(), paymentStatus);
        payment.setStatus(paymentStatus);
        paymentEffect.applyEffectIfPaid(payment);
        payment.setAmount(payloadRequest.paidAmount());

        payment.setRawWebhook(raw);
        payment.setPaymentMethodName(payloadRequest.paymentMethod());
        payment.setPaymentChannel(payloadRequest.paymentChannel());
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);
    }

}
