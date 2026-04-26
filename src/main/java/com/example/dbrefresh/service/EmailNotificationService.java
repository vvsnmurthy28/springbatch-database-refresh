package com.example.dbrefresh.service;

import com.example.dbrefresh.model.TableRefreshStatus;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for sending email notifications after batch job completion
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

    private final JavaMailSender mailSender;

    @Value("${batch.job.notification.email}")
    private String notificationEmail;

    @Value("${batch.job.notification.enabled:true}")
    private boolean notificationEnabled;

    @Value("${spring.mail.username}")
    private String senderEmail;

    /**
     * Send job execution summary via email
     */
    public void sendJobCompletionEmail(List<TableRefreshStatus> results, long totalExecutionTime) {
        if (!notificationEnabled) {
            log.info("Email notification is disabled");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail);
            helper.setTo(notificationEmail);
            helper.setSubject(buildEmailSubject(results));
            helper.setText(buildEmailContent(results, totalExecutionTime), true);

            mailSender.send(message);
            log.info("Email notification sent successfully to {}", notificationEmail);
        } catch (Exception e) {
            log.error("Failed to send email notification", e);
        }
    }

    /**
     * Build email subject line
     */
    private String buildEmailSubject(List<TableRefreshStatus> results) {
        long successCount = results.stream().filter(TableRefreshStatus::isSuccess).count();
        long failureCount = results.size() - successCount;

        String status = failureCount == 0 ? "SUCCESS" : "PARTIAL SUCCESS";
        return String.format("[DB REFRESH] %s - %d Successful, %d Failed", 
                status, successCount, failureCount);
    }

    /**
     * Build HTML email content
     */
    private String buildEmailContent(List<TableRefreshStatus> results, long totalExecutionTime) {
        long successCount = results.stream().filter(TableRefreshStatus::isSuccess).count();
        long failureCount = results.size() - successCount;
        long totalRowsCopied = results.stream().mapToLong(TableRefreshStatus::getRowsCopied).sum();

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family: Arial, sans-serif;'>");
        html.append("<h2 style='color: #333;'>Database Refresh Batch Job - Completion Report</h2>");
        html.append("<p style='color: #666;'>Timestamp: ").append(timestamp).append("</p>");

        // Summary Section
        html.append("<div style='background-color: #f5f5f5; padding: 15px; margin: 20px 0; border-radius: 5px;'>");
        html.append("<h3>Job Summary</h3>");
        html.append("<table style='width: 100%; border-collapse: collapse;'>");
        html.append("<tr><td style='padding: 8px;'><b>Total Tables:</b></td><td>").append(results.size()).append("</td></tr>");
        html.append("<tr><td style='padding: 8px;'><b>Successful:</b></td><td style='color: green;'><b>").append(successCount).append("</b></td></tr>");
        html.append("<tr><td style='padding: 8px;'><b>Failed:</b></td><td style='color: red;'><b>").append(failureCount).append("</b></td></tr>");
        html.append("<tr><td style='padding: 8px;'><b>Total Rows Copied:</b></td><td>").append(totalRowsCopied).append("</td></tr>");
        html.append("<tr><td style='padding: 8px;'><b>Total Execution Time:</b></td><td>").append(formatTime(totalExecutionTime)).append("</td></tr>");
        html.append("</table>");
        html.append("</div>");

        // Details Section
        html.append("<h3>Table-wise Details</h3>");
        html.append("<table style='width: 100%; border-collapse: collapse; border: 1px solid #ddd;'>");
        html.append("<thead style='background-color: #f0f0f0;'>");
        html.append("<tr>");
        html.append("<th style='border: 1px solid #ddd; padding: 10px; text-align: left;'>Table Name</th>");
        html.append("<th style='border: 1px solid #ddd; padding: 10px; text-align: left;'>Status</th>");
        html.append("<th style='border: 1px solid #ddd; padding: 10px; text-align: left;'>Rows Copied</th>");
        html.append("<th style='border: 1px solid #ddd; padding: 10px; text-align: left;'>Time (ms)</th>");
        html.append("<th style='border: 1px solid #ddd; padding: 10px; text-align: left;'>Error Message</th>");
        html.append("</tr>");
        html.append("</thead>");
        html.append("<tbody>");

        for (TableRefreshStatus result : results) {
            String statusColor = result.isSuccess() ? "#28a745" : "#dc3545";
            String statusText = result.isSuccess() ? "✓ SUCCESS" : "✗ FAILED";

            html.append("<tr>");
            html.append("<td style='border: 1px solid #ddd; padding: 10px;'>").append(result.getTableName()).append("</td>");
            html.append("<td style='border: 1px solid #ddd; padding: 10px; color: ").append(statusColor).append(";'><b>").append(statusText).append("</b></td>");
            html.append("<td style='border: 1px solid #ddd; padding: 10px;'>").append(result.getRowsCopied()).append("</td>");
            html.append("<td style='border: 1px solid #ddd; padding: 10px;'>").append(result.getExecutionTimeMs()).append("</td>");
            html.append("<td style='border: 1px solid #ddd; padding: 10px;'>");
            if (!result.isSuccess() && result.getErrorMessage() != null) {
                html.append(result.getErrorMessage());
            }
            html.append("</td>");
            html.append("</tr>");
        }

        html.append("</tbody>");
        html.append("</table>");

        // Footer
        html.append("<p style='color: #999; margin-top: 30px; font-size: 12px;'>");
        html.append("This is an automated email from DB Refresh Batch System. Please do not reply to this email.");
        html.append("</p>");
        html.append("</body></html>");

        return html.toString();
    }

    /**
     * Format milliseconds to human-readable format
     */
    private String formatTime(long milliseconds) {
        long minutes = milliseconds / 60000;
        long seconds = (milliseconds % 60000) / 1000;
        return String.format("%d min %d sec", minutes, seconds);
    }
}

