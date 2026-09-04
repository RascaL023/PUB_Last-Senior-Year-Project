package id.my.rascal.imagekit.internal.controller;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import id.my.rascal.image.api.ImageRegistryApi;
import id.my.rascal.image.api.ImageWebhookApiPayload;
import id.my.rascal.image.api.event.ImageCreatedEvent;
import id.my.rascal.image.api.event.ImageDeletedEvent;
import id.my.rascal.image.api.event.ImageUpdatedEvent;
import id.my.rascal.imagekit.internal.service.ImageKitService;

@RestController
@RequestMapping("/api/v1/images/imagekit")
public class ImageKitWebhookController {

    private static final Logger log = LoggerFactory.getLogger(ImageKitWebhookController.class);

    private final ImageKitService imageKitService;
    private final ImageRegistryApi imageRegistryApi;
    private final ApplicationEventPublisher eventPublisher;

    public ImageKitWebhookController(ImageKitService imageKitService, ImageRegistryApi imageRegistryApi, ApplicationEventPublisher eventPublisher) {
        this.imageKitService = imageKitService;
        this.imageRegistryApi = imageRegistryApi;
        this.eventPublisher = eventPublisher;
    }

    @PostMapping("/webhooks")
    public ResponseEntity<Void> imagekit(
        @RequestHeader HttpHeaders headers,
        @RequestBody String rawPayload
    ) {
        Map<String, String> headerMap = new HashMap<>();
        headers.forEach((name, values) -> {
            if (!values.isEmpty())
                headerMap.put(name, values.get(0));
        });

        ImageWebhookApiPayload payload;
        log.warn("Webhook detected!");
        try {
            payload = imageKitService.verifyAndParseWebhook(rawPayload, headerMap, null);
        } catch (Exception e) {
            log.warn("Rejected ImageKit webhook: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        }
        log.info("ImageKit webhook payload: {}", rawPayload);

        resolveEventType(payload);
        return ResponseEntity.ok().build();
    }


    private void resolveEventType(ImageWebhookApiPayload payload) {
        switch (payload.eventType().toLowerCase()) {
            case "file.created" -> {
                imageRegistryApi.registerOrUpdate(payload.fileId(), payload.filePath());
                log.info("Publishing ImageCreatedEvent: filePath={}, fileId={}", payload.filePath(), payload.fileId());
                eventPublisher.publishEvent(
                    new ImageCreatedEvent(payload.filePath(), payload.fileId(), payload.url()));
            }
            case "file.updated" -> {
                String previousFilePath = imageRegistryApi.registerOrUpdate(payload.fileId(), payload.filePath());
                log.info("Publishing ImageUpdatedEvent: filePath={}, previousFilePath={}, fileId={}", payload.filePath(), previousFilePath, payload.fileId());
                eventPublisher.publishEvent(
                    new ImageUpdatedEvent(payload.filePath(), previousFilePath, payload.fileId(), payload.url()));
            }
            case "file.deleted" -> {
                String filePath = imageRegistryApi.resolveAndDelete(payload.fileId());
                if (filePath == null)
                    filePath = payload.filePath();
                log.info("Publishing ImageDeletedEvent: filePath={}, fileId={}", filePath, payload.fileId());
                eventPublisher.publishEvent(
                    new ImageDeletedEvent(filePath, payload.fileId(), payload.url()));
            }
            default -> log.info("Ignored ImageKit webhook event: {}", payload.eventType());
        }
    }

}
