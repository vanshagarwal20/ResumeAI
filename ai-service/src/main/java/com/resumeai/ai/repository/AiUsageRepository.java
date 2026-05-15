package com.resumeai.ai.repository;

import com.resumeai.ai.entity.AiUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiUsageRepository extends JpaRepository<AiUsage, Integer> {

    Optional<AiUsage> findByUserIdAndUsageMonthAndUsageYear(Integer userId, Integer usageMonth, Integer usageYear);
}

