-- src/main/resources/custom-indexes.sql

ALTER TABLE jobs ADD FULLTEXT INDEX ft_jobs_name_desc (name, description);

ALTER TABLE online_resumes ADD FULLTEXT INDEX ft_resumes_text (title, full_name, summary, certifications, educations, languages);

ALTER TABLE users ADD FULLTEXT INDEX ft_users_name_address (name, address);

CREATE INDEX idx_users_is_vip ON users (is_vip);