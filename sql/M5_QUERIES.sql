USE alumni_db;
-- 1. Leaderboard top 5
SELECT name, role, total_score FROM v_impact ORDER BY total_score DESC LIMIT 5;
-- 2. Rank (MySQL 8)
SELECT RANK() OVER (ORDER BY total_score DESC) AS rnk, name, total_score FROM v_impact;
-- 3. Current badge of every user
SELECT v.name, v.total_score, COALESCE(b.name,'None') AS badge
FROM v_impact v LEFT JOIN badges b
  ON b.min_score = (SELECT MAX(min_score) FROM badges WHERE min_score <= v.total_score);
-- 4. Badges awarded (3-table JOIN)
SELECT u.full_name, b.name, a.awarded_at
FROM achievements a JOIN users u ON u.id=a.user_id JOIN badges b ON b.id=a.badge_id;
-- 5. Reports per status
SELECT status, COUNT(*) AS total FROM reports GROUP BY status;
-- 6. Reports per target type
SELECT target_type, COUNT(*) AS total FROM reports GROUP BY target_type;
-- 7. Most reported users
SELECT u.full_name, COUNT(*) AS times_reported
FROM reports r JOIN users u ON u.id=r.reported_id GROUP BY u.id HAVING COUNT(*) >= 1 ORDER BY times_reported DESC;
-- 8. Monthly activity across members' modules
SELECT m, kind, COUNT(*) AS total FROM (
  SELECT DATE_FORMAT(created_at,'%Y-%m') m, 'Mentorship requests' kind FROM mentorship_requests
  UNION ALL SELECT DATE_FORMAT(created_at,'%Y-%m'), 'Referral requests' FROM referrals
  UNION ALL SELECT DATE_FORMAT(created_at,'%Y-%m'), 'Discussion posts' FROM discussion_posts
  UNION ALL SELECT DATE_FORMAT(created_at,'%Y-%m'), 'Questions asked' FROM qa_questions) x
GROUP BY m, kind ORDER BY m, kind;
-- 9. Feedback count and average rating
SELECT COUNT(*) AS total_feedback, ROUND(AVG(rating),2) AS avg_rating FROM feedback;
-- 10. Users with zero impact
SELECT name, role FROM v_impact WHERE total_score = 0;
-- 11. Score by role
SELECT role, SUM(total_score) AS score, AVG(total_score) AS avg_score FROM v_impact GROUP BY role;
