-- Embeddings from BGE-M3 have 1024 dimensions. Older databases may already
-- have this column, added at startup by the previous code.
ALTER TABLE file_record ADD COLUMN IF NOT EXISTS embedding vector(1024);

CREATE INDEX IF NOT EXISTS idx_file_record_embedding
    ON file_record USING hnsw (embedding vector_cosine_ops);

UPDATE file_record SET download_count = 0 WHERE download_count IS NULL;
ALTER TABLE file_record ALTER COLUMN download_count SET DEFAULT 0;
ALTER TABLE file_record ALTER COLUMN download_count SET NOT NULL;

UPDATE file_record SET is_active = true WHERE is_active IS NULL;
ALTER TABLE file_record ALTER COLUMN is_active SET DEFAULT true;
ALTER TABLE file_record ALTER COLUMN is_active SET NOT NULL;

ALTER TABLE file_record ALTER COLUMN description TYPE varchar(1000);
ALTER TABLE course ALTER COLUMN description TYPE varchar(1000);

-- Filled by V3, which normalizes with the same Java code search uses.
ALTER TABLE course_alias ADD COLUMN IF NOT EXISTS normalized_alias varchar(255);

-- Foreign keys that are not already the leading column of a unique constraint.
CREATE INDEX IF NOT EXISTS idx_department_faculty ON department (faculty_id);
CREATE INDEX IF NOT EXISTS idx_course_alias_course ON course_alias (course_id);
CREATE INDEX IF NOT EXISTS idx_lecturer_department ON lecturer (department_id);
CREATE INDEX IF NOT EXISTS idx_student_department ON student (department_id);
CREATE INDEX IF NOT EXISTS idx_course_lecturer_lecturer ON course_lecturer (lecturer_id);
CREATE INDEX IF NOT EXISTS idx_enrollment_course ON enrollment (course_id);
CREATE INDEX IF NOT EXISTS idx_file_record_course ON file_record (course_id);
CREATE INDEX IF NOT EXISTS idx_file_record_uploaded_by ON file_record (uploaded_by_id);
