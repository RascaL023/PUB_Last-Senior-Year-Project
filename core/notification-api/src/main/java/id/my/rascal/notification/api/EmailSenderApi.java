package id.my.rascal.notification.api;

public interface EmailSenderApi {

    EmailSendResultApi sendEmail(SendEmailRequestApi request);

}
