package com.sp.SwimmingPool.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendVerificationEmail(String to, String code) {
        System.out.println("Attempting to send verification email to: " + to);
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Email Verification");
            message.setText("Your verification code is: " + code + "\n\n" +
                    "This code will expire in 24 hours.");

            System.out.println("Email content prepared. From: " + fromEmail + ", To: " + to);
            mailSender.send(message);
            System.out.println("Verification email sent successfully to: " + to);
        } catch (Exception e) {
            System.out.println("Failed to send verification email to: " + to + ". Error: " + e.getMessage() + e);
            throw e; // Re-throw to handle it in the controller
        }
    }

    @Async
    public void sendHealthReportReminder(String to) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("Health Report Required");
        message.setText("Please upload your health report within 5 days to complete your registration.");
        mailSender.send(message);
    }

    @Async
    public void sendRegistrationRejection(String to, String reason) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("Registration Status Update");
        message.setText("Your registration has been rejected.\n\nReason: " + reason +
                "\n\nPlease address the issue and try again.");
        mailSender.send(message);
    }

    @Async
    public void sendRegistrationApproval(String to) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject("Registration Approved");
        message.setText("Your registration has been approved! You can now log in to your account.");
        mailSender.send(message);
    }
}