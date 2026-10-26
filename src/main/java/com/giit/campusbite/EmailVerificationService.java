package com.giit.campusbite;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class EmailVerificationService {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailVerificationService(
            JavaMailSender mailSender,
            @Value("${campusbite.mail-from:}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    public void sendCode(String email, String code) {
        if (fromAddress.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Email verification is not configured. Contact the canteen administrator.");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject("Verify your CampusBite account");
        message.setText("Your CampusBite verification code is " + code
                + ". It expires in 15 minutes. If you did not request this, ignore this email.");
        try {
            mailSender.send(message);
        } catch (MailException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Could not send the account verification email", exception);
        }
    }
}
