package com.resumeai.export.repository;

import com.resumeai.export.entity.ExportJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExportJobRepository extends JpaRepository<ExportJob, Integer> {

    List<ExportJob> findByUserIdOrderByCreatedAtDesc(Integer userId);

    List<ExportJob> findByUserIdAndResumeIdOrderByCreatedAtDesc(Integer userId, Integer resumeId);
}
