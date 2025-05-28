package com.sp.SwimmingPool.service;

import jakarta.mail.internet.MimeMessage;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MimeMessage mimeMessage;


    private EmailService emailService;

    // Sample HTML template with all placeholders
    private static final String TEMPLATE = """
        <html>
        <head><title>{{TITLE}}</title></head>
        <body>
            <h1>{{TITLE}}</h1>
            <p>{{MAIN_MESSAGE}}</p>
            <!-- VERIFICATION_BLOCK_START -->
            <div>
                <span>Kodunuz: {{VERIFICATION_CODE}}</span>
            </div>
            <!-- VERIFICATION_BLOCK_END -->
            <!-- SUCCESS_ICON_BLOCK_START -->
            <!-- <div class="success-icon">Başarılı!</div> -->
            <!-- SUCCESS_ICON_BLOCK_END -->
            <footer>{{HELP_TEXT}}</footer>
        </body>
        </html>
        """;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        emailService = spy(new EmailService(mailSender));
        // Set private fields via reflection
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@example.com");
        ReflectionTestUtils.setField(emailService, "frontendUrl", "https://frontend.com");

        // Mock mailSender to return a mock MimeMessage
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    // --- Public API tests (with template) ---

    @Test
    void testSendVerificationEmail() {
        doReturn(TEMPLATE).when(emailService).readEmailTemplate();
        emailService.sendVerificationEmail("user@example.com", "123456");
        verify(mailSender, timeout(1000)).send(any(MimeMessage.class));
    }

    @Test
    void testSendPasswordResetEmail() {
        doReturn(TEMPLATE).when(emailService).readEmailTemplate();
        emailService.sendPasswordResetEmail("reset@example.com", "654321");
        verify(mailSender, timeout(1000)).send(any(MimeMessage.class));
    }

    @Test
    void testSendStaffInvitation() {
        doReturn(TEMPLATE).when(emailService).readEmailTemplate();
        emailService.sendStaffInvitation("staff@example.com", "token123");
        verify(mailSender, timeout(1000)).send(any(MimeMessage.class));
    }

    @Test
    void testSendHealthReportReminder() {
        doReturn(TEMPLATE).when(emailService).readEmailTemplate();
        emailService.sendHealthReportReminder("health@example.com");
        verify(mailSender, timeout(1000)).send(any(MimeMessage.class));
    }

    @Test
    void testSendRegistrationRejection() {
        doReturn(TEMPLATE).when(emailService).readEmailTemplate();
        emailService.sendRegistrationRejection("reject@example.com", "Eksik belge");
        verify(mailSender, timeout(1000)).send(any(MimeMessage.class));
    }

    @Test
    void testSendRegistrationApproval() {
        doReturn(TEMPLATE).when(emailService).readEmailTemplate();
        emailService.sendRegistrationApproval("approve@example.com");
        verify(mailSender, timeout(1000)).send(any(MimeMessage.class));
    }

    @Test
    void testSendInvalidDocumentNotification() {
        doReturn(TEMPLATE).when(emailService).readEmailTemplate();
        emailService.sendInvalidDocumentNotification("invalid@example.com", "Ali", "Geçersiz rapor");
        verify(mailSender, timeout(1000)).send(any(MimeMessage.class));
    }

    // --- Error/edge cases for public API ---

    @Test
    void testSendVerificationEmailWithNullTemplate() {
        doReturn(null).when(emailService).readEmailTemplate();
        // Should not throw, should not send
        emailService.sendVerificationEmail("user@example.com", "123456");
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void testSendEmailHandlesMailSenderException() {
        doReturn(TEMPLATE).when(emailService).readEmailTemplate();
        doThrow(new RuntimeException("Mail error")).when(mailSender).send(any(MimeMessage.class));
        // Should not throw
        emailService.sendVerificationEmail("user@example.com", "123456");
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    // --- Private method: prepareEmailContent ---

    @Test
    @SneakyThrows
    void testPrepareEmailContent_AllBlocks() {
        Method method = EmailService.class.getDeclaredMethod(
                "prepareEmailContent",
                String.class, String.class, String.class, String.class, String.class, boolean.class
        );
        method.setAccessible(true);

        // All blocks shown
        String result = (String) method.invoke(
                emailService,
                TEMPLATE,
                "Başlık",
                "Ana mesaj",
                "Yardım metni",
                "CODE123",
                true
        );
        assertNotNull(result);
        assertTrue(result.contains("Başlık"));
        assertTrue(result.contains("Ana mesaj"));
        assertTrue(result.contains("CODE123"));
        assertTrue(result.contains("success-icon"));
        assertTrue(result.contains("Yardım metni"));
    }

    @Test
    @SneakyThrows
    void testPrepareEmailContent_NoVerificationBlock() {
        Method method = EmailService.class.getDeclaredMethod(
                "prepareEmailContent",
                String.class, String.class, String.class, String.class, String.class, boolean.class
        );
        method.setAccessible(true);

        // No code, so verification block should be removed
        String result = (String) method.invoke(
                emailService,
                TEMPLATE,
                "Başlık",
                "Ana mesaj",
                "Yardım metni",
                null,
                false
        );
        assertNotNull(result);
        assertFalse(result.contains("{{VERIFICATION_CODE}}"));
        assertFalse(result.contains("Kodunuz:"));
        assertFalse(result.contains("success-icon"));
    }

    @Test
    @SneakyThrows
    void testPrepareEmailContent_NoSuccessIconBlock() {
        Method method = EmailService.class.getDeclaredMethod(
                "prepareEmailContent",
                String.class, String.class, String.class, String.class, String.class, boolean.class
        );
        method.setAccessible(true);

        // No success icon
        String result = (String) method.invoke(
                emailService,
                TEMPLATE,
                "Başlık",
                "Ana mesaj",
                "Yardım metni",
                "CODE123",
                false
        );
        assertNotNull(result);
        assertTrue(result.contains("CODE123"));
        assertFalse(result.contains("success-icon"));
    }

    @Test
    @SneakyThrows
    void testPrepareEmailContent_NullTemplate() {
        Method method = EmailService.class.getDeclaredMethod(
                "prepareEmailContent",
                String.class, String.class, String.class, String.class, String.class, boolean.class
        );
        method.setAccessible(true);

        String result = (String) method.invoke(
                emailService,
                null,
                "Başlık",
                "Ana mesaj",
                "Yardım metni",
                "CODE123",
                true
        );
        assertNull(result);
    }

    @Test
    void testSendEmail_ContentNull() {
        // Use reflection to call protected sendEmail
        ReflectionTestUtils.invokeMethod(
                emailService,
                "sendEmail",
                "to@example.com",
                "Subject",
                null,
                "Type"
        );
        verify(mailSender, never()).send(any(MimeMessage.class));
    }
}