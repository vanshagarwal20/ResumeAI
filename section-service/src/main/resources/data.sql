-- =====================================================================================
-- Fix: Ensure section_type column is VARCHAR(50) and NOT a MySQL ENUM.
-- Hibernate 6 (Spring Boot 3.2) with @Enumerated(EnumType.STRING) may create
-- a native MySQL ENUM column that only contains the enum values known at table
-- creation time. If EXTRA_CURRICULAR was added later, MySQL rejects the insert.
--
-- This script runs on every startup AFTER Hibernate DDL (defer-datasource-initialization).
-- It converts the column to VARCHAR(50) which accepts any valid string.
-- Running ALTER on an already-VARCHAR column is a safe no-op.
-- =====================================================================================

-- Convert section_type from ENUM to VARCHAR(50) if it isn't already
ALTER TABLE resume_sections MODIFY COLUMN section_type VARCHAR(50) NOT NULL;

-- Ensure is_visible column is properly typed
ALTER TABLE resume_sections MODIFY COLUMN is_visible TINYINT(1) NOT NULL DEFAULT 1;
