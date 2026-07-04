package com.terrarent.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendVerificationEmail(String recipientEmail, String verificationCode) {
        log.info("Sending verification email to {} with code: {}", recipientEmail, verificationCode);
        
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(recipientEmail);
        message.setSubject("Your TerraRent Verification Code");
        message.setText("Your verification code is: " + verificationCode);
        
        try {
            mailSender.send(message);
            log.info("Email sent successfully!");
        } catch (Exception e) {
            log.error("Failed to send verification email to {}", recipientEmail, e);
            throw new RuntimeException("Could not send verification email: " + e.getMessage(), e);
        }
    }
}