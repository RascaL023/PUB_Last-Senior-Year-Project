package id.my.rascal.notification.api;

import java.util.Map;

public record SendEmailRequestApi(
    String to,
    String subject,
    String html,
    String text,
    Map<String, String> headers
) {}
