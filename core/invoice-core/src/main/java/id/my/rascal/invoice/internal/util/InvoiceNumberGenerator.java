package id.my.rascal.invoice.internal.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.function.Predicate;

public class InvoiceNumberGenerator {

    private InvoiceNumberGenerator() {}

    private static final String INVOICE_PREFIX = "INV-";
    private static final String RANDOM_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final Random RANDOM = new Random();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("ddMMyyyy");

    public static String generateUniqueInvoiceNumber(Predicate<String> existsByNumber) {
        for (int i = 0; i < 10; i++) {
            String candidate = generateInvoiceNumber();
            if (!existsByNumber.test(candidate))
                return candidate;
        }
        throw new IllegalStateException("Failed to generate unique invoice number");
    }

    public static String generateInvoiceNumber() {
        return INVOICE_PREFIX + LocalDateTime.now().format(DATE_FORMAT) + "-" + randomSuffix(6);
    }

    private static String randomSuffix(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++)
            sb.append(RANDOM_CHARS.charAt(RANDOM.nextInt(RANDOM_CHARS.length())));
        return sb.toString();
    }

}
