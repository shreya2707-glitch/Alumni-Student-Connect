-- =====================================================================
--  DATABASE CONTRACT  (Member 1 owns the tables marked [M1])
--  RULE FOR ALL MEMBERS: any table that refers to a person must use
--  a foreign key to users(id). Never store names/emails in your own tables.
-- =====================================================================
CREATE DATABASE IF NOT EXISTS alumni_db;
USE alumni_db;

-- [M1] one row per account (student, alumni or admin)
CREATE TABLE IF NOT EXISTS users (
    id            INT PRIMARY KEY AUTO_INCREMENT,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(200) NOT NULL,              -- salt:PBKDF2 hash, never plain text
    user_role     ENUM('STUDENT','ALUMNI','ADMIN') NOT NULL,
    full_name     VARCHAR(100) NOT NULL,
    email         VARCHAR(120) NOT NULL,
    bio           TEXT,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- [M1] 1:1 extension tables (shared primary key = users.id)
CREATE TABLE IF NOT EXISTS alumni_profiles (
    user_id       INT PRIMARY KEY,
    company       VARCHAR(100),
    job_role      VARCHAR(100),
    industry      VARCHAR(50),
    grad_year     INT,
    skills        VARCHAR(255),                       -- comma separated
    interests     VARCHAR(255),
    goal          VARCHAR(50),
    qualification VARCHAR(150),
    journey       VARCHAR(400),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS student_profiles (
    user_id         INT PRIMARY KEY,
    skills          VARCHAR(255),
    interests       VARCHAR(255),
    target_industry VARCHAR(50),
    goal            VARCHAR(50),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ---------------------------------------------------------------------
-- Other members: add your tables BELOW this line in your own section.
-- Example of the expected pattern:
--   CREATE TABLE mentorship_requests (            -- [M2]
--       id INT PRIMARY KEY AUTO_INCREMENT,
--       student_id INT NOT NULL, alumni_id INT NOT NULL,
--       status ENUM('PENDING','ACCEPTED','DECLINED') DEFAULT 'PENDING',
--       FOREIGN KEY (student_id) REFERENCES users(id),
--       FOREIGN KEY (alumni_id)  REFERENCES users(id));
-- ---------------------------------------------------------------------
-- [M2] mentorship, roadmaps, progress
-- [M3] jobs, applications, referrals
-- [M4] discussions, questions, events, messages, notifications
-- [M5] achievements, leaderboard, moderation, analytics
