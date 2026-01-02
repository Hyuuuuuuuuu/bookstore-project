package com.hutech.bookstore.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.exceptions.TemplateInputException;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    public void sendEmailVerificationCode(String toEmail, String verificationCode, String userName) {
        try {
            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("verificationCode", verificationCode);
            context.setVariable("validMinutes", 15); // Code valid for 15 minutes

            String htmlContent = templateEngine.process("email-verification", context);

            sendHtmlEmail(toEmail, "Email Verification - BookStore", htmlContent);
            log.info("Email verification sent to: {}", toEmail);

        } catch (TemplateInputException tie) {
            // Template not found -> fallback to plain text email to avoid 500
            String textBody = String.format("Hello %s,\n\nYour verification code is: %s\nIt is valid for %d minutes.\n\nRegards,\nBookStore",
                    userName != null ? userName : "", verificationCode, 15);
            log.warn("Template 'email-verification' not found, falling back to plain text email for {}: {}", toEmail, tie.getMessage());
            sendSimpleEmail(toEmail, "Email Verification - BookStore", textBody);
        } catch (Exception e) {
            log.error("Failed to send email verification to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send email verification", e);
        }
    }

    public void sendPasswordResetOTP(String toEmail, String otp, String userName) {
        try {
            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("otp", otp);
            context.setVariable("validMinutes", 10); // OTP valid for 10 minutes

            String htmlContent = templateEngine.process("password-reset", context);

            sendHtmlEmail(toEmail, "Password Reset - BookStore", htmlContent);
            log.info("Password reset OTP sent to: {}", toEmail);

        } catch (TemplateInputException tie) {
            String textBody = String.format("Hello %s,\n\nYour password reset code is: %s\nIt is valid for %d minutes.\n\nRegards,\nBookStore",
                    userName != null ? userName : "", otp, 10);
            log.warn("Template 'password-reset' not found, falling back to plain text email for {}: {}", toEmail, tie.getMessage());
            sendSimpleEmail(toEmail, "Password Reset - BookStore", textBody);
        } catch (Exception e) {
            log.error("Failed to send password reset OTP to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send password reset OTP", e);
        }
    }

    public void sendWelcomeEmail(String toEmail, String userName) {
        try {
            Context context = new Context();
            context.setVariable("userName", userName);

            String htmlContent = templateEngine.process("welcome", context);

            sendHtmlEmail(toEmail, "Welcome to BookStore!", htmlContent);
            log.info("Welcome email sent to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send welcome email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send welcome email", e);
        }
    }

    public void sendOrderConfirmation(String toEmail, String userName, String orderNumber, double totalAmount) {
        try {
            Context context = new Context();
            context.setVariable("userName", userName);
            context.setVariable("orderNumber", orderNumber);
            context.setVariable("totalAmount", String.format("%.0f", totalAmount));

            String htmlContent = templateEngine.process("order-confirmation", context);

            sendHtmlEmail(toEmail, "Order Confirmation - #" + orderNumber, htmlContent);
            log.info("Order confirmation sent to: {}", toEmail);

        } catch (Exception e) {
            log.error("Failed to send order confirmation to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Failed to send order confirmation", e);
        }
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        helper.setFrom("noreply@bookstore.com");

        mailSender.send(message);
    }

    // Simple text email method for basic notifications
    public void sendSimpleEmail(String to, String subject, String text) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(text, false);
            helper.setFrom("noreply@bookstore.com");

            mailSender.send(message);
            log.info("Simple email sent to: {}", to);

        } catch (Exception e) {
            log.error("Failed to send simple email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
