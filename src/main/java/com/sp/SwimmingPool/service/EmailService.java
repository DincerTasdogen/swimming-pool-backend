package com.sp.SwimmingPool.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private String readEmailTemplate() {
        try {
            ClassPathResource resource = new ClassPathResource("templates/email-template.html");
            return new String(Files.readAllBytes(Paths.get(resource.getURI())), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.out.println("Error reading email template: " + e.getMessage());
            return null;
        }
    }

    private String prepareEmailContent(String code, String title, String message, String subMessage) {
        String template = readEmailTemplate();
        if (template == null) {
            return null;
        }

        String content = template
                .replace("Şifrenizi mi unuttunuz?", title)
                .replace("Merak etmeyin, bu çok kolay çözülebilir! Güvenliğiniz için şifre sıfırlama işleminizi hızlıca tamamlayabilirsiniz.", message)
                .replace("<span class=\"highlight\">15 dakika</span> içinde geçerli olan bu kodu kullanarak şifrenizi sıfırlayabilirsiniz. Eğer bu talebi siz yapmadıysanız lütfen hesabınızı kontrol edin.", subMessage);

        if (code != null && !code.isEmpty()) {
            content = content.replace("789432", code);
        } else {
            // Remove the verification code section for emails without a code
            content = content.replace("<div class=\"verification-container\">", "<div class=\"verification-container\" style=\"display:none;\">");
        }

        return content;
    }

    @Async
    public void sendVerificationEmail(String to, String code) {
        System.out.println("Attempting to send verification email to: " + to);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("E-posta Doğrulama");

            String content = prepareEmailContent(
                    code,
                    "E-posta Adresinizi Doğrulayın",
                    "Hesabınızı etkinleştirmek için lütfen aşağıdaki doğrulama kodunu kullanın.",
                    "<span class=\"highlight\">15 dakika</span> içinde geçerli olan bu kodu kullanarak hesabınızı doğrulayabilirsiniz."
            );

            if (content != null) {
                helper.setText(content, true);
                System.out.println("Email content prepared. From: " + fromEmail + ", To: " + to);
                mailSender.send(message);
                System.out.println("Verification email sent successfully to: " + to);
            } else {
                throw new RuntimeException("Email template could not be loaded");
            }
        } catch (Exception e) {
            System.out.println("Failed to send verification email to: " + to + ". Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Async
    public void sendPasswordResetEmail(String to, String code) {
        System.out.println("Attempting to send password reset email to: " + to);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Şifre Sıfırlama İsteği");

            String content = prepareEmailContent(
                    code,
                    "Şifrenizi mi unuttunuz?",
                    "Merak etmeyin, bu çok kolay çözülebilir! Güvenliğiniz için şifre sıfırlama işleminizi hızlıca tamamlayabilirsiniz.",
                    "<span class=\"highlight\">15 dakika</span> içinde geçerli olan bu kodu kullanarak şifrenizi sıfırlayabilirsiniz. Eğer bu talebi siz yapmadıysanız lütfen hesabınızı kontrol edin."
            );

            if (content != null) {
                helper.setText(content, true);
                System.out.println("Email content prepared. From: " + fromEmail + ", To: " + to);
                mailSender.send(message);
                System.out.println("Password reset email sent successfully to: " + to);
            } else {
                throw new RuntimeException("Email template could not be loaded");
            }
        } catch (Exception e) {
            System.out.println("Failed to send password reset email to: " + to + ". Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Async
    public void sendHealthReportReminder(String to) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Sağlık Raporu Hatırlatması");

            String content = prepareEmailContent(
                    "",
                    "Sağlık Raporu Gerekli",
                    "Kaydınızı tamamlamak için lütfen 5 gün içinde sağlık raporunuzu yükleyin.",
                    "Havuzu kullanabilmek için güncel sağlık raporunuzu sisteme yüklemeniz gerekmektedir."
            );

            if (content != null) {
                helper.setText(content, true);
                mailSender.send(message);
            } else {
                throw new RuntimeException("Email template could not be loaded");
            }
        } catch (Exception e) {
            System.out.println("Failed to send health report reminder email to: " + to + ". Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Async
    public void sendRegistrationRejection(String to, String reason) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Kayıt Durumu Güncellemesi");

            String content = prepareEmailContent(
                    "",
                    "Kayıt Reddedildi",
                    "Üzgünüz, kayıt talebiniz reddedildi.",
                    "Sebep: <span class=\"highlight\">" + reason + "</span><br>Lütfen sorunu çözün ve tekrar deneyin."
            );

            if (content != null) {
                helper.setText(content, true);
                mailSender.send(message);
            } else {
                throw new RuntimeException("Email template could not be loaded");
            }
        } catch (Exception e) {
            System.out.println("Failed to send registration rejection email to: " + to + ". Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Async
    public void sendRegistrationApproval(String to) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Kayıt Onaylandı");

            String content = prepareEmailContent(
                    "",
                    "Kayıt Onaylandı",
                    "Tebrikler! Kayıt talebiniz onaylandı.",
                    "Artık hesabınıza giriş yapabilir ve hizmetlerimizden yararlanabilirsiniz."
            );

            // Add success icon for approval emails
            if (content != null) {
                // Replace commented success icon with actual implementation
                content = content.replace("<!-- Success Icon Example (commented out by default) -->", "");
                content = content.replace("<!--", ""); // Remove opening comment
                content = content.replace("-->", ""); // Remove closing comment

                helper.setText(content, true);
                mailSender.send(message);
            } else {
                throw new RuntimeException("Email template could not be loaded");
            }
        } catch (Exception e) {
            System.out.println("Failed to send registration approval email to: " + to + ". Error: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }
}