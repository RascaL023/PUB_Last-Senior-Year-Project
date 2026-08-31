package id.my.rascal.payment.internal.integration.xendit;

public class XenditClientException extends RuntimeException {

    public XenditClientException(String message) {
        super(message);
    }

    public XenditClientException(String message, Throwable cause) {
        super(message, cause);
    }

}
