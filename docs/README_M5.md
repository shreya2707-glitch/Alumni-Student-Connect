# Member 5: Admin & Community Impact (setup)

## 1. Database (run in this order)
1. schema.sql  2. M2_SCHEMA.sql  3. M3_SCHEMA.sql  4. member4_schema.sql
5. sql/M5_SCHEMA.sql          (tables: badges, achievements, reports, blocked_users, feedback + view v_impact)
6. sql/M5_DEMO_DATA.sql       (optional: gives other members' tables a little data so the leaderboard shows scores)
Users must already exist (run the app once so SampleData creates them) before step 6.

## 2. Code
- Copy ALL files from src/ into the team's flat project folder (next to Main.java, mysql-connector.jar).
- Do NOT replace AppContext.java. Follow integration/INTEGRATION_STEPS.md (one line + optional block check).
- Run run_m5.bat (Windows) or run_m5.sh: compiles, runs 39 tests, opens the Admin Console.

## 3. In the full app
Login as admin / admin123 -> "Admin Console" tab (Leaderboard, Badges, Moderation, Analytics, Feedback).

## Score sources (real data)
Mentorship x20 <- M2 mentorship_requests (ACCEPTED) | Helpful answer x10 <- M4 qa_answers (is_helpful)
Referral x15 <- M3 referrals (ACCEPTED)            | Event x5 <- M4 event_rsvps
