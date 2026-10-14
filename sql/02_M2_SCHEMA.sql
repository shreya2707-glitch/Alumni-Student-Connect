-- Member 2: Mentorship & Career Quest tables.
-- Run after the main schema.sql has created alumni_db and users.
USE alumni_db;

CREATE TABLE IF NOT EXISTS mentorship_requests (
    id INT PRIMARY KEY AUTO_INCREMENT,
    student_id INT NOT NULL,
    alumni_id INT NOT NULL,
    message VARCHAR(500) NOT NULL,
    status ENUM('PENDING','ACCEPTED','DECLINED') NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (alumni_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS career_roadmaps (
    id INT PRIMARY KEY AUTO_INCREMENT,
    student_id INT NOT NULL,
    title VARCHAR(150) NOT NULL,
    target_role VARCHAR(120) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS roadmap_milestones (
    id INT PRIMARY KEY AUTO_INCREMENT,
    roadmap_id INT NOT NULL,
    title VARCHAR(150) NOT NULL,
    description VARCHAR(500),
    is_completed BOOLEAN NOT NULL DEFAULT FALSE,
    FOREIGN KEY (roadmap_id) REFERENCES career_roadmaps(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS career_quests (
    id INT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(150) NOT NULL UNIQUE,
    description VARCHAR(500) NOT NULL,
    points INT NOT NULL DEFAULT 10
);

CREATE TABLE IF NOT EXISTS student_quests (
    student_id INT NOT NULL,
    quest_id INT NOT NULL,
    status ENUM('IN_PROGRESS','COMPLETED') NOT NULL DEFAULT 'IN_PROGRESS',
    completed_at TIMESTAMP NULL,
    PRIMARY KEY (student_id, quest_id),
    FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (quest_id) REFERENCES career_quests(id) ON DELETE CASCADE
);

INSERT IGNORE INTO career_quests(title, description, points) VALUES
('Polish Your Resume', 'Create or improve a one-page resume.', 10),
('Build a Portfolio Project', 'Complete a small project related to your target role.', 20),
('Practise an Interview', 'Practise answering five common interview questions.', 15),
('Connect with a Professional', 'Prepare three thoughtful questions for an alumnus.', 10),
('Learn a New Skill', 'Finish one beginner lesson in a useful technical skill.', 15);
