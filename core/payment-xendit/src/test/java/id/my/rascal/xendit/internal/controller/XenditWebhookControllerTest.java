package id.my.rascal.xendit.internal.controller;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.ObjectMapper;

import id.my.rascal.common.exception.BadRequestException;
import id.my.rascal.payment.api.PaymentApi;
import id.my.rascal.xendit.internal.component.XenditClient;
import id.my.rascal.xendit.internal.component.XenditProperties;
import id.my.rascal.xendit.internal.service.XenditService;

class XenditWebhookControllerTest {

    @Test
    void malformedPayload_acknowledgedWithoutThrowing() {
        XenditService service = new XenditService(
            mock(XenditProperties.class),
            mock(XenditClient.class),
            mock(PaymentApi.class),
            new ObjectMapper()
        );

        assertDoesNotThrow(() -> service.handleWebhook("{not-json"));
    }

    @Test
    void deterministicFailure_acknowledgedWithOk() {
        XenditService service = mock(XenditService.class);
        whenValidToken(service);
        doThrow(new BadRequestException("stale")).when(service).handleWebhook(anyString());

        XenditWebhookController controller = new XenditWebhookController(service);

        assertEquals(HttpStatus.OK, controller.xendit("token", "{}").getStatusCode());
    }

    @Test
    void transientFailure_returnsServerErrorForRetry() {
        XenditService service = mock(XenditService.class);
        whenValidToken(service);
        doThrow(new RuntimeException("db down")).when(service).handleWebhook(anyString());

        XenditWebhookController controller = new XenditWebhookController(service);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, controller.xendit("token", "{}").getStatusCode());
    }

    @Test
    void happyPath_returnsOk() {
        XenditService service = mock(XenditService.class);
        whenValidToken(service);

        XenditWebhookController controller = new XenditWebhookController(service);

        assertEquals(HttpStatus.OK, controller.xendit("token", "{}").getStatusCode());
        verify(service).handleWebhook("{}");
    }

    private void whenValidToken(XenditService service) {
        org.mockito.Mockito.when(service.isValidToken("token")).thenReturn(true);
    }

}
