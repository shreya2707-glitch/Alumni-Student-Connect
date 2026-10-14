-- OPTIONAL: fills the OTHER members' tables with a little activity so the leaderboard has scores for the demo.
-- Run after M5_SCHEMA.sql. Safe to skip if your real app already has data.
USE alumni_db;
INSERT INTO mentorship_requests(student_id, alumni_id, message, status)
  SELECT s.id, a.id, 'Demo mentorship', 'ACCEPTED'
  FROM (SELECT MIN(id) id FROM users WHERE user_role='STUDENT') s, users a WHERE a.user_role='ALUMNI';
INSERT INTO mentorship_requests(student_id, alumni_id, message, status)
  SELECT s.id, a.id, 'Demo mentorship 2', 'ACCEPTED'
  FROM (SELECT MIN(id) id FROM users WHERE user_role='STUDENT') s, users a WHERE a.user_role='ALUMNI' AND MOD(a.id,2)=1;
INSERT IGNORE INTO event_rsvps(event_id, user_id)
  SELECT e.id, u.id FROM campus_events e, users u WHERE u.user_role <> 'ADMIN';
INSERT INTO jobs(title, company, job_type, location, required_skills, description)
  SELECT 'Demo Developer', 'DemoCorp', 'JOB', 'Pune', 'Java,SQL', 'Demo job'
  WHERE NOT EXISTS (SELECT 1 FROM jobs);
INSERT INTO referrals(student_id, alumni_id, job_id, message, status)
  SELECT s.id, a.id, (SELECT MIN(id) FROM jobs), 'Demo referral', 'ACCEPTED'
  FROM (SELECT MIN(id) id FROM users WHERE user_role='STUDENT') s, users a WHERE a.user_role='ALUMNI' AND MOD(a.id,2)=1;
