package com.resumeai.ai.repository;

import com.resumeai.ai.entity.AiRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiRequestRepository extends JpaRepository<AiRequest, Integer> {

    List<AiRequest> findByUserIdOrderByCreatedAtDesc(Integer userId);
}
