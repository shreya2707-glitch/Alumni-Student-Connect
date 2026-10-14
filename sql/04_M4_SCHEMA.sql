-- =====================================================================
--  MEMBER 4 SCHEMA: Campus Connect & Live Activities
--  Safe Sandbox: Uses CREATE TABLE IF NOT EXISTS without foreign keys
--  to existing Member 1-3 tables, ensuring zero merge conflicts.
-- =====================================================================
CREATE DATABASE IF NOT EXISTS alumni_db;
USE alumni_db;

-- 1. Discussion Circles (Topic-based community boards)
CREATE TABLE IF NOT EXISTS discussion_circles (
    id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255) NOT NULL,
    icon_tag VARCHAR(50) DEFAULT 'GENERAL'
);

-- 2. Discussion Posts (Asynchronously batched chat messages)
CREATE TABLE IF NOT EXISTS discussion_posts (
    id INT PRIMARY KEY AUTO_INCREMENT,
    circle_id INT NOT NULL,
    author_id INT NOT NULL,
    user_id INT GENERATED ALWAYS AS (author_id) VIRTUAL,
    title VARCHAR(200) DEFAULT '',
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. Live Q&A Questions asked by Students
CREATE TABLE IF NOT EXISTS qa_questions (
    id INT PRIMARY KEY AUTO_INCREMENT,
    student_id INT NOT NULL,
    user_id INT GENERATED ALWAYS AS (student_id) VIRTUAL,
    target_industry VARCHAR(100) DEFAULT 'General',
    title VARCHAR(200) NOT NULL,
    details TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. Live Q&A Answers from Alumni
CREATE TABLE IF NOT EXISTS qa_answers (
    id INT PRIMARY KEY AUTO_INCREMENT,
    question_id INT NOT NULL,
    alumni_id INT NOT NULL,
    user_id INT GENERATED ALWAYS AS (alumni_id) VIRTUAL,
    answer_text TEXT NOT NULL,
    is_helpful BOOLEAN NOT NULL DEFAULT FALSE,
    helpful_count INT NOT NULL DEFAULT 0
);

-- 5. Campus Events (Webinars, workshops, mock interviews)
CREATE TABLE IF NOT EXISTS campus_events (
    id INT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(180) NOT NULL,
    description TEXT,
    event_type VARCHAR(50) DEFAULT 'WORKSHOP',
    event_date VARCHAR(60) NOT NULL,
    seats_left INT NOT NULL DEFAULT 50
);

-- 6. Event RSVPs (Ensuring one seat reservation per student per event)
CREATE TABLE IF NOT EXISTS event_rsvps (
    id INT PRIMARY KEY AUTO_INCREMENT,
    event_id INT NOT NULL,
    user_id INT NOT NULL,
    UNIQUE KEY uq_event_user (event_id, user_id)
);

-- 7. Live Interactive Event Polls
CREATE TABLE IF NOT EXISTS event_polls (
    id INT PRIMARY KEY AUTO_INCREMENT,
    event_id INT NOT NULL,
    question VARCHAR(255) NOT NULL,
    option_a VARCHAR(120) DEFAULT '',
    option_b VARCHAR(120) DEFAULT '',
    option_c VARCHAR(120) DEFAULT '',
    votes_a INT NOT NULL DEFAULT 0,
    votes_b INT NOT NULL DEFAULT 0,
    votes_c INT NOT NULL DEFAULT 0
);

-- 8. Poll Votes
CREATE TABLE IF NOT EXISTS poll_votes (
    id INT PRIMARY KEY AUTO_INCREMENT,
    poll_id INT NOT NULL,
    user_id INT NOT NULL,
    option_index INT NOT NULL,
    UNIQUE KEY uq_poll_user (poll_id, user_id)
);

-- 9. In-App User Notifications
CREATE TABLE IF NOT EXISTS user_notifications (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    message VARCHAR(300) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================================
--  PRE-SEEDED MOCK DATA (Supplied by Member 4)
-- =====================================================================

-- 1. Pre-seed Discussion Circles
INSERT IGNORE INTO discussion_circles (id, name, description, icon_tag) VALUES
(1, 'AI & Machine Learning', 'Discussions on ML models, research, careers, and industry trends.', 'AI_ML'),
(2, 'Placement & Interview Prep', 'Interview experiences, coding problem breakdowns, resume reviews, and HR strategies.', 'PLACEMENTS'),
(3, 'Startups & Entrepreneurship', 'Idea validation, tech stacks for MVPs, funding, and founding stories.', 'STARTUPS');

-- 2. Pre-seed Discussion Posts (Assuming user IDs 1 and 2 exist in your demo)
INSERT IGNORE INTO discussion_posts (id, circle_id, author_id, title, content) VALUES
(1, 1, 1, 'Best resources for learning PyTorch?', 'Does anyone have good tutorial recommendations for someone switching from TensorFlow to PyTorch?'),
(2, 1, 2, 'Stanford CS231n is great', 'Highly recommend the Stanford CS231n lectures on YouTube. They cover the math and the implementation perfectly.'),
(3, 2, 1, 'Amazon OA Next Week', 'Just got the link for the Amazon Online Assessment. Are there any specific graph algorithms I should focus on?');

-- 3. Pre-seed Q&A Questions
INSERT IGNORE INTO qa_questions (id, student_id, target_industry, title, details) VALUES
(1, 1, 'Software Engineering', 'How important is System Design for entry-level roles?', 'I am graduating next year and have been doing mostly LeetCode. Do I need to study System Design for junior roles?'),
(2, 2, 'Data Science', 'Should I get an AWS Certification?', 'Does having an AWS Machine Learning specialty cert actually help get resume shortlisted?');

-- 4. Pre-seed Q&A Answers (Notice is_helpful=1 to show off Member 5's leaderboard hook!)
INSERT IGNORE INTO qa_answers (id, question_id, alumni_id, answer_text, is_helpful, helpful_count) VALUES
(1, 1, 3, 'For junior (L3/SDE1) roles, it is rarely asked. Focus 90% of your time on Data Structures and Algorithms. However, knowing basic concepts like Load Balancing and Caching can make you stand out in the final behavioral/technical round.', 1, 15),
(2, 2, 4, 'Certifications won\'t guarantee a job, but they help pass the initial HR filter. Building a real end-to-end project hosted on AWS is much more valuable than just the certificate.', 0, 3);

-- 5. Pre-seed Campus Events
INSERT IGNORE INTO campus_events (id, title, description, event_type, event_date, seats_left) VALUES
(1, 'Resume Roasting & Review Session', 'Submit your resume and have our alumni recruiters review it live.', 'WORKSHOP', '2026-10-20 18:00:00', 45),
(2, 'Mock System Design Interview', 'Live mock interview focusing on designing a scalable chat application.', 'MOCK_INTERVIEW', '2026-10-25 17:00:00', 50);

-- 6. Pre-seed Event Polls
INSERT IGNORE INTO event_polls (id, event_id, question, option_a, option_b, option_c, votes_a, votes_b, votes_c) VALUES
(1, 1, 'What is your biggest struggle with resumes?', 'Formatting & ATS', 'Writing good bullet points', 'Lack of projects', 12, 28, 5);
