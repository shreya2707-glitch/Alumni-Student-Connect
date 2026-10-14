-- Member 3: Opportunity Arena tables.
-- Run after the main schema.sql has created alumni_db and users.
-- Sample jobs are inserted automatically by the application on first run when the jobs table is empty.
USE alumni_db;

CREATE TABLE IF NOT EXISTS jobs (
    id INT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(150) NOT NULL,
    company VARCHAR(100) NOT NULL,
    job_type ENUM('JOB','INTERNSHIP') NOT NULL DEFAULT 'JOB',
    location VARCHAR(100),
    required_skills VARCHAR(255),                 -- comma separated, same style as users' skills
    description TEXT,
    posted_by INT NULL,                           -- alumnus who posted it; NULL = placement cell / seeded
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (posted_by) REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS applications (
    id INT PRIMARY KEY AUTO_INCREMENT,
    student_id INT NOT NULL,
    job_id INT NOT NULL,
    status ENUM('APPLIED','SHORTLISTED','INTERVIEW','REJECTED') NOT NULL DEFAULT 'APPLIED',
    applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    notes VARCHAR(300),
    UNIQUE KEY uq_application (student_id, job_id),
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS referrals (
    id INT PRIMARY KEY AUTO_INCREMENT,
    student_id INT NOT NULL,
    alumni_id INT NOT NULL,
    job_id INT NOT NULL,
    message VARCHAR(500) NOT NULL,
    status ENUM('PENDING','ACCEPTED','REJECTED') NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (alumni_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE
);

-- Save / Skip choices for the swipe cards (one row per student per job)
CREATE TABLE IF NOT EXISTS job_actions (
    student_id INT NOT NULL,
    job_id INT NOT NULL,
    action ENUM('SAVED','SKIPPED') NOT NULL,
    PRIMARY KEY (student_id, job_id),
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE
);
