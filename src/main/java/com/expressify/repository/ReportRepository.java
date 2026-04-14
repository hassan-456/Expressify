package com.expressify.repository;

import com.expressify.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    List<Report> findByReporterIdOrderByCreatedAtDesc(Long reporterId);

    boolean existsByReporterIdAndContentTypeAndContentId(Long reporterId,
                                                         Report.ContentType contentType,
                                                         Long contentId);

    long countByContentTypeAndContentId(Report.ContentType contentType, Long contentId);
}
