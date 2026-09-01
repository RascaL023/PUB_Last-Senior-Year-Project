package id.my.rascal.xendit.internal.exception;

public class XenditClientException extends RuntimeException {

    public XenditClientException(String message) {
        super(message);
    }

    public XenditClientException(String message, Throwable cause) {
        super(message, cause);
    }

}
