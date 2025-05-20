package com.sp.SwimmingPool.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

@Slf4j
@Service
@RequiredArgsConstructor // Lombok annotation for constructor injection of final fields
public class EmailService {

    private final JavaMailSender mailSender; // Marked final for constructor injection

    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * Reads the HTML email template from the classpath.
     *
     * @return The template content as a String, or null if an error occurs.
     */
    private String readEmailTemplate() {
        try {
            ClassPathResource resource = new ClassPathResource("templates/email-template.html");
            // Ensure it reads with UTF-8
            return Files.readString(Paths.get(resource.getURI()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Error reading email template: {}", e.getMessage(), e); // Log the full error
            return null; // Return null to indicate failure
        }
    }

    /**
     * Prepares the email content by replacing placeholders in the template.
     *
     * @param template        The HTML template content.
     * @param title           The text for the {{TITLE}} placeholder.
     * @param mainMessage     The text for the {{MAIN_MESSAGE}} placeholder.
     * @param helpText        The text for the {{HELP_TEXT}} placeholder.
     * @param code            The verification code for {{VERIFICATION_CODE}}, or null/empty to hide the block.
     * @param showSuccessIcon True to show the success icon block, false to hide it.
     * @return The processed HTML content, or null if the template is null.
     */
    private String prepareEmailContent(String template, String title, String mainMessage, String helpText, String code, boolean showSuccessIcon) {
        if (template == null) {
            log.error("Cannot prepare email content because the template is null.");
            return null;
        }

        // Basic replacements
        String content = template
                .replace("{{TITLE}}", title != null ? title : "")
                .replace("{{MAIN_MESSAGE}}", mainMessage != null ? mainMessage : "")
                .replace("{{HELP_TEXT}}", helpText != null ? helpText : "");

        // Handle verification code block
        if (code != null && !code.isBlank()) {
            content = content.replace("{{VERIFICATION_CODE}}", code);
            // Remove comments to show the block
            content = content.replace("<!-- VERIFICATION_BLOCK_START -->", "");
            content = content.replace("<!-- VERIFICATION_BLOCK_END -->", "");
            log.debug("Verification code block included.");
        } else {
            // Remove the entire verification block using regex for multiline content
            // (?s) enables DOTALL mode, where '.' matches line terminators
            content = content.replaceAll("(?s)<!-- VERIFICATION_BLOCK_START -->.*?<!-- VERIFICATION_BLOCK_END -->", "");
            log.debug("Verification code block removed.");
        }

        // Handle success icon block
        if (showSuccessIcon) {
            // Remove comments to show the block
            content = content.replace("<!-- SUCCESS_ICON_BLOCK_START -->", "");
            content = content.replace("<!-- SUCCESS_ICON_BLOCK_END -->", "");
            // Ensure the inner content (like the img tag) is uncommented if necessary
            // This assumes the template has the icon structure commented like: <!-- <img ... /> -->
            content = content.replace("<!-- <div class=\"success-icon\">", "<div class=\"success-icon\">");
            content = content.replace("</div> -->", "</div>");
            log.debug("Success icon block included.");
        } else {
            // Remove the entire success icon block
            content = content.replaceAll("(?s)<!-- SUCCESS_ICON_BLOCK_START -->.*?<!-- SUCCESS_ICON_BLOCK_END -->", "");
            log.debug("Success icon block removed.");
        }

        return content;
    }

    /**
     * Sends an email asynchronously.
     *
     * @param to      Recipient email address.
     * @param subject Email subject.
     * @param content HTML content of the email.
     * @param type    A string describing the type of email for logging purposes (e.g., "Verification").
     */
    @Async // Ensures email sending doesn't block the main thread
    protected void sendEmail(String to, String subject, String content, String type) {
        if (content == null) {
            log.error("Cannot send {} email to {}: content is null (template likely failed to load/process).", type, to);
            return;
        }
        log.info("Attempting to send {} email to: {}", type, to);
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);

            // Replace the subject placeholder in the final content
            String finalContent = content.replace("{{SUBJECT}}", subject != null ? subject : "");
            helper.setText(finalContent, true); // true indicates HTML content

            mailSender.send(message);
            log.info("{} email sent successfully to: {}", type, to);

        } catch (Exception e) {
            log.error("Failed to send {} email to: {}. Error: {}", type, to, e.getMessage(), e);
        }
    }

    public void sendVerificationEmail(String to, String code) {
        String subject = "E-posta Doğrulama";
        String template = readEmailTemplate();
        String helpText = "<span class=\"highlight\">15 dakika</span> içinde geçerli olan bu kodu kullanarak hesabınızı doğrulayabilirsiniz.";
        String content = prepareEmailContent(
                template,
                "E-posta Adresinizi Doğrulayın",
                "Hesabınızı etkinleştirmek için lütfen aşağıdaki doğrulama kodunu kullanın.",
                helpText,
                code,  // Pass the code
                false // No success icon
        );
        sendEmail(to, subject, content, "Verification");
    }

    public void sendPasswordResetEmail(String to, String code) {
        String subject = "Şifre Sıfırlama İsteği";
        String template = readEmailTemplate();
        String helpText = "<span class=\"highlight\">15 dakika</span> içinde geçerli olan bu kodu kullanarak şifrenizi sıfırlayabilirsiniz. Eğer bu talebi siz yapmadıysanız lütfen hesabınızı kontrol edin.";
        String content = prepareEmailContent(
                template,
                "Şifrenizi mi unuttunuz?",
                "Merak etmeyin, bu çok kolay çözülebilir! Güvenliğiniz için şifre sıfırlama işleminizi hızlıca tamamlayabilirsiniz.",
                helpText,
                code,  // Pass the code
                false // No success icon
        );
        sendEmail(to, subject, content, "Password Reset");
    }

    public void sendStaffInvitation(String to, String token) {
        String subject = "Yüzme Havuzu Sistemi - Personel Daveti";
        String activationLink = "http://localhost:3000/activate-staff?token=" + token; // Ensure correct frontend URL
        String template = readEmailTemplate();

        // Specific help text for the invitation email including the activation link
        String helpText = "Lütfen <span class=\"highlight\">7 gün</span> içinde bu bağlantıya tıklayarak şifrenizi oluşturun: " +
                "<br><a href=\"" + activationLink + "\" style=\"color: #6a42c2; font-weight: bold; text-decoration: none; padding: 8px 16px; background-color: #f5f0ff; border-radius: 8px; display: inline-block; margin-top: 10px;\">Hesabımı Aktifleştir</a>";

        String content = prepareEmailContent(
                template,
                "Personel Hesabınız Oluşturuldu",
                "Yüzme Havuzu Yönetim Sistemi ekibine hoş geldiniz. Aşağıdaki bağlantıyı kullanarak hesabınızı aktifleştirebilir ve şifrenizi belirleyebilirsiniz.",
                helpText, // Use the specific help text with the link
                null,     // No verification code needed
                false     // No success icon
        );

        // Log the link separately for easier debugging if needed
        if (content != null) {
            log.info("Activation link generated for staff invitation to {}: {}", to, activationLink);
        }

        sendEmail(to, subject, content, "Staff Invitation");
    }

    public void sendHealthReportReminder(String to) {
        String subject = "Sağlık Raporu Hatırlatması";
        String template = readEmailTemplate();
        String helpText = "Havuzu kullanabilmek için güncel sağlık raporunuzu sisteme yüklemeniz gerekmektedir.";
        String content = prepareEmailContent(
                template,
                "Sağlık Raporu Gerekli",
                "Kaydınızı tamamlamak için lütfen 5 gün içinde sağlık raporunuzu yükleyin.",
                helpText,
                null,  // No code
                false // No success icon
        );
        sendEmail(to, subject, content, "Health Report Reminder");
    }

    public void sendRegistrationRejection(String to, String reason) {
        String subject = "Kayıt Durumu Güncellemesi";
        String template = readEmailTemplate();
        // Sanitize reason or ensure it's safe HTML if it comes from user input
        String helpText = "Sebep: <span class=\"highlight\">" + (reason != null ? reason : "Belirtilmedi") + "</span><br>Lütfen sorunu çözün ve tekrar deneyin veya destek ile iletişime geçin.";
        String content = prepareEmailContent(
                template,
                "Kayıt Reddedildi",
                "Üzgünüz, kayıt talebiniz onaylanamadı.",
                helpText,
                null,  // No code
                false // No success icon
        );
        sendEmail(to, subject, content, "Registration Rejection");
    }

    public void sendRegistrationApproval(String to) {
        String subject = "Kayıt Onaylandı";
        String template = readEmailTemplate();
        String helpText = "Artık hesabınıza giriş yapabilir ve hizmetlerimizden yararlanabilirsiniz.";
        String content = prepareEmailContent(
                template,
                "Kayıt Onaylandı!", // More cheerful title
                "Tebrikler! Yüzme havuzu kayıt talebiniz onaylandı.",
                helpText,
                null, // No code
                true  // Show success icon
        );
        sendEmail(to, subject, content, "Registration Approval");
    }

    public void sendInvalidDocumentNotification(String to, String memberName, String reason) {
        String subject = "Sağlık Raporunuz Hakkında Aksiyon Gerekli!";
        String template = readEmailTemplate();
        String mainMessage = "Merhaba " + (memberName != null ? memberName : "Değerli Üyemiz") +
                ",<br><br>Yüklediğiniz sağlık raporunu gözden geçirdik. Ne yazık ki sağlık raporunuzu kabul edemiyoruz.";
        String helpText = "Sebep: <span class=\"highlight\">" + (reason != null ? reason : "Belirtilmedi") + "</span>" +
                "<br><br>Lütfen hesabınıza girerek sağlık raporunuzu yeniden yükleyin. " +
                "Yüklemeden önce lütfen sağlık raporunuzun okunabilir olduğundan ve tüm gereksinimleri karşıladığından emin olun." +
                "<br>Daha fazla sorunuz varsa iletişime geçmekten çekinmeyin.";

        String content = prepareEmailContent(
                template,
                "Sağlık Raporunuz Hakkında",
                mainMessage,
                helpText,
                null,
                false
        );
        sendEmail(to, subject, content, "Invalid Medical Document");
    }
}
