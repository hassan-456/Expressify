package com.expressify.service;

import com.expressify.entity.Post;
import com.expressify.entity.Report;
import com.expressify.entity.User;
import com.expressify.repository.PostRepository;
import com.expressify.repository.ReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

@Service
public class ReportService {

    private static final int REPORT_THRESHOLD = 5;

    private static final Set<String> ALLOWED_REASONS = new HashSet<>(Arrays.asList(
            "Spam",
            "Harassment or bullying",
            "Hate speech",
            "Nudity or sexual content",
            "Violence or dangerous acts",
            "Misinformation",
            "Other"
    ));

    private final ReportRepository reportRepository;
    private final PostRepository postRepository;
    private final AdminNotificationService adminNotificationService;

    public ReportService(ReportRepository reportRepository,
                         PostRepository postRepository,
                         AdminNotificationService adminNotificationService) {
        this.reportRepository = reportRepository;
        this.postRepository = postRepository;
        this.adminNotificationService = adminNotificationService;
    }

    @Transactional
    public void reportPost(User reporter, Long postId, String reason) {
        if (reporter == null || postId == null) {
            throw new IllegalArgumentException("Invalid report request");
        }
        String trimmedReason = reason != null ? reason.trim() : "";
        if (trimmedReason.isEmpty()) {
            throw new IllegalArgumentException("Please select a reason to report this post.");
        }
        if (!isAllowedReason(trimmedReason)) {
            throw new IllegalArgumentException("Invalid report reason.");
        }

        if (reportRepository.existsByReporterIdAndContentTypeAndContentId(
                reporter.getId(), Report.ContentType.post, postId)) {
            throw new IllegalArgumentException("You have already reported this post.");
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("Post not found."));

        Report report = new Report();
        report.setReporterId(reporter.getId());
        report.setContentType(Report.ContentType.post);
        report.setContentId(postId);
        report.setReason(trimmedReason);
        reportRepository.save(report);

        long totalReports = reportRepository.countByContentTypeAndContentId(Report.ContentType.post, postId);
        if (totalReports >= REPORT_THRESHOLD) {
            String message = "Post ID " + postId + " has received " + totalReports +
                    " reports and needs review.";
            adminNotificationService.notifyAdmins("post_report_threshold", message, postId);
        }
    }

    private boolean isAllowedReason(String reason) {
        for (String allowed : ALLOWED_REASONS) {
            if (allowed.equalsIgnoreCase(reason)) {
                return true;
            }
        }
        return false;
    }
}

