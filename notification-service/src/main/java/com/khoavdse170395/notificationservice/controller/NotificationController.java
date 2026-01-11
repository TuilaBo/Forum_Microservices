package com.khoavdse170395.notificationservice.controller;

import com.khoavdse170395.notificationservice.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller để test email service (optional).
 */
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    @Autowired
    private EmailService emailService;

    /**
     * Test endpoint để gửi email.
     */
    @PostMapping("/test-email")
    public ResponseEntity<String> testEmail(
            @RequestParam String to,
            @RequestParam String subject,
            @RequestParam String body) {
        try {
            emailService.sendEmail(to, subject, body);
            return ResponseEntity.ok("Email sent successfully to: " + to);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to send email: " + e.getMessage());
        }
    }

    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Notification Service is running");
    }
}
