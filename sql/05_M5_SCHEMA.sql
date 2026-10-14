-- ===== MEMBER 5: Admin / Community Impact =====
-- RUN ORDER: schema.sql -> M2_SCHEMA.sql -> M3_SCHEMA.sql -> member4_schema.sql -> THIS FILE
-- Score data comes from the other members' REAL tables (no manual activity table):
--   Mentorship x20     <- M2 mentorship_requests (alumni_id, status='ACCEPTED')
--   Helpful answer x10 <- M4 qa_answers (alumni_id, is_helpful=1)
--   Referral x15       <- M3 referrals (alumni_id, status='ACCEPTED')
--   Event x5           <- M4 event_rsvps (user_id)
USE alumni_db;

DROP VIEW  IF EXISTS v_impact;
DROP TABLE IF EXISTS achievements, blocked_users, reports, feedback, badges, community_activity;

CREATE TABLE badges (
  id        INT AUTO_INCREMENT PRIMARY KEY,
  name      VARCHAR(30) NOT NULL UNIQUE,
  min_score INT NOT NULL CHECK (min_score >= 0)
);

CREATE TABLE achievements (
  id         INT AUTO_INCREMENT PRIMARY KEY,
  user_id    INT NOT NULL,
  badge_id   INT NOT NULL,
  awarded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE (user_id, badge_id),
  FOREIGN KEY (user_id)  REFERENCES users(id)  ON DELETE CASCADE,
  FOREIGN KEY (badge_id) REFERENCES badges(id) ON DELETE CASCADE
);

-- A report can target a user, or content from Member 4 (Q&A answer / discussion post).
-- reported_id is always the AUTHOR (a users FK). M4 tables have no PK we can FK to, so target_id is a plain INT.
CREATE TABLE reports (
  id          INT AUTO_INCREMENT PRIMARY KEY,
  reporter_id INT NOT NULL,
  reported_id INT NOT NULL,
  target_type ENUM('USER','QA_ANSWER','DISCUSSION_POST') NOT NULL DEFAULT 'USER',
  target_id   INT NULL,
  reason      VARCHAR(500) NOT NULL,
  status      ENUM('PENDING','RESOLVED','DISMISSED') NOT NULL DEFAULT 'PENDING',
  admin_id    INT NULL,
  admin_note  VARCHAR(255),
  created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  resolved_at TIMESTAMP NULL,
  CHECK (reporter_id <> reported_id),
  FOREIGN KEY (reporter_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (reported_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (admin_id)    REFERENCES users(id) ON DELETE SET NULL
);

CREATE TABLE blocked_users (
  user_id    INT PRIMARY KEY,
  reason     VARCHAR(255),
  blocked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE feedback (
  id         INT AUTO_INCREMENT PRIMARY KEY,
  user_id    INT NOT NULL,
  message    VARCHAR(1000) NOT NULL,
  rating     TINYINT NOT NULL CHECK (rating BETWEEN 1 AND 5),
  status     ENUM('NEW','REVIEWED','ACTIONED') NOT NULL DEFAULT 'NEW',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Leaderboard = VIEW. Score = M*20 + H*10 + R*15 + E*5
CREATE VIEW v_impact AS
SELECT t.*, (t.mentorships*20 + t.helpful_answers*10 + t.referrals*15 + t.events*5) AS total_score
FROM (
  SELECT u.id AS user_id, u.full_name AS name, u.user_role AS role,
    (SELECT COUNT(*) FROM mentorship_requests m WHERE m.alumni_id=u.id AND m.status='ACCEPTED') AS mentorships,
    (SELECT COUNT(*) FROM qa_answers a WHERE a.alumni_id=u.id AND a.is_helpful=1)               AS helpful_answers,
    (SELECT COUNT(*) FROM referrals r WHERE r.alumni_id=u.id AND r.status='ACCEPTED')           AS referrals,
    (SELECT COUNT(*) FROM event_rsvps e WHERE e.user_id=u.id)                                   AS events
  FROM users u WHERE u.user_role <> 'ADMIN'
) t;

INSERT INTO badges(name,min_score) VALUES ('Bronze',20),('Silver',50),('Gold',100),('Platinum',200);

INSERT INTO feedback(user_id,message,rating)
  SELECT id,'Mentor matching is very useful',5 FROM users WHERE user_role='STUDENT' LIMIT 1;
INSERT INTO feedback(user_id,message,rating)
  SELECT id,'Please add more filters in discovery',3 FROM users WHERE user_role='ALUMNI' LIMIT 1;
INSERT INTO reports(reporter_id,reported_id,target_type,target_id,reason)
  SELECT s.id, a.id, 'USER', a.id, 'Sent spam messages'
  FROM (SELECT MIN(id) id FROM users WHERE user_role='STUDENT') s, (SELECT MIN(id) id FROM users WHERE user_role='ALUMNI') a
  WHERE s.id IS NOT NULL AND a.id IS NOT NULL;
