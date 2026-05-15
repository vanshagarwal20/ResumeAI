package com.resumeai.section.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ReorderSectionsRequest {

    @Valid
    @NotEmpty(message = "sections list cannot be empty")
    private List<SectionOrderItem> sections;

    @Data
    public static class SectionOrderItem {

        @NotNull(message = "sectionId is required")
        private Integer sectionId;

        @NotNull(message = "displayOrder is required")
        @Min(value = 1, message = "displayOrder must be at least 1")
        private Integer displayOrder;
    }
}
