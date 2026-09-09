package id.my.rascal.xendit.internal.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.xendit.internal.service.XenditService;

@RestController
@RequestMapping("/api/v1/payments/webhooks")
public class XenditWebhookController {

    private final XenditService xenditService;

    public XenditWebhookController(
        XenditService xenditService
    ) {
        this.xenditService = xenditService;
    }

    @PostMapping("/xendit")
    public ResponseEntity<Void> xendit(
        @RequestHeader(value = "X-Callback-Token", required = false) String callbackToken,
        @RequestBody String rawPayload
    ) {
        // System.out.println(rawPayload);
        if (!xenditService.isValidToken(callbackToken))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        try {
            xenditService.handleWebhook(rawPayload);
        } catch (BadRequestException e) {
            // Deterministik: payload valid-tapi-tak-dapat-diproses → ack agar Xendit berhenti retry.
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            // Need to be retried
            return ResponseEntity.internalServerError().build();
        }

        return ResponseEntity.ok().build();
    }

}
