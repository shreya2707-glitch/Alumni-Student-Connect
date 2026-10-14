# Member 5: Admin & Community Impact Module (Documentation)
Smart Alumni Network | Java Swing, JDBC, MySQL

## 1. Setup
1. DB order: schema.sql, M2_SCHEMA.sql, M3_SCHEMA.sql, member4_schema.sql, sql/M5_SCHEMA.sql, (optional) sql/M5_DEMO_DATA.sql. Run the main app once first so SampleData creates users.
2. Copy src/*.java into the team's flat project folder (next to mysql-connector.jar).
3. AppContext.init(): add `modules.add(new AdminModule());`
4. Replace AuthService.java with integration/AuthService.java (adds the block check).
5. Run run_m5.bat (compiles, runs 39 tests, opens Admin Console). Full app: login admin / admin123.

## 2. Features
Leaderboard, Badges, Moderation (users + Member 4 content), Analytics, Feedback. All screens load data on a background thread (SwingWorker) so the window never freezes.

## 3. Community Impact Score (real data)
| Activity | Weight | Source |
|---|---|---|
| Mentorship | x20 | M2 mentorship_requests (status ACCEPTED, by alumni_id) |
| Helpful answer | x10 | M4 qa_answers (is_helpful = 1, by alumni_id) |
| Referral | x15 | M3 referrals (status ACCEPTED, by alumni_id) |
| Event | x5 | M4 event_rsvps (by user_id) |
Total Score -> Badge (Bronze 20, Silver 50, Gold 100, Platinum 200) -> Leaderboard.
Example: 2 mentorships + 3 answers + 1 referral + 4 events = 40+30+15+20 = 105 = Gold.

## 4. Flowchart
Login (M1) -> ADMIN? -> AdminModule -> Admin Dashboard
 |- Leaderboard: v_impact view -> sort by score -> badge
 |- Badges: thresholds + achievements (Award Badges)
 |- Moderation: report PENDING -> Resolve / Dismiss / Block user / Delete content
 |- Analytics: COUNT / AVG / GROUP BY across M2, M3, M4 tables
 |- Feedback: NEW -> REVIEWED -> ACTIONED
Button click -> panel method -> Ui.async (SwingWorker) -> Repository -> JDBC PreparedStatement -> MySQL -> model objects -> JTable.

## 5. Class diagram explanation
- User (M1, abstract) -> Student, Alumni, Admin (inheritance).
- AdminModule implements FeatureModule (interface/polymorphism); visible only for Role.ADMIN.
- Models: Badge, Achievement, CommunityImpact, Report, Feedback (encapsulation).
- Repositories: ImpactRepository, ReportRepository, FeedbackRepository (all SQL here).
- Logic: ImpactCalculator (score + badge rule), Validator (input), BlockCheck (used by AuthService).
- GUI: AdminDashboardPanel contains LeaderboardPanel, BadgePanel, ModerationPanel, AnalyticsPanel, FeedbackPanel (composition). Ui = shared helpers incl. async.

## 6. ER explanation (Member 5 tables)
| Table | Key columns | Foreign keys |
|---|---|---|
| badges | id, name, min_score | none |
| achievements | id, user_id, badge_id, UNIQUE(user_id,badge_id) | users, badges |
| reports | id, reporter_id, reported_id, target_type, target_id, status, admin_id | 3 FKs to users |
| blocked_users | user_id (PK) | users |
| feedback | id, user_id, rating, status | users |
View v_impact (the leaderboard) reads users + M2 mentorship_requests + M3 referrals + M4 qa_answers, event_rsvps.
Reports can target a USER, or M4 content (QA_ANSWER / DISCUSSION_POST). M4 tables have no FKs by design, so target_id is a plain INT and reported_id stores the content's author (FK to users).

## 7. SQL explanation
View with correlated subqueries; CHECK (rating 1-5, reporter <> reported); ENUM statuses; UNIQUE; ON DELETE CASCADE; GROUP BY / HAVING / AVG / SUM; UNION ALL for monthly activity; RANK() window function; INSERT IGNORE for safe badge awarding; transactions (block + resolve, delete content + resolve); PreparedStatement everywhere.

## 8. Testing (M5Tests, 39 checks)
Score formula; badge boundaries (19 None, 20 Bronze, 99 Silver, 100 Gold); invalid input (empty, too long, letters, 0, rating 6/9); leaderboard sorted and excludes admins; badge award twice = no duplicates; score follows real tables (M2 accepted +20, pending +0, M4 helpful +10, event +5, M3 referral +15); report create/resolve/close-twice/invalid status/self-report/unknown user; block; M4 content report + delete; feedback save + rating validation; analytics. Login-block checked separately (blocked user rejected, normal user and wrong password unchanged). Manual: ADMIN sees Admin Console, STUDENT/ALUMNI do not; stop MySQL -> message, no crash.

## 9. Demo sequence
1. Show tables + v_impact in Workbench. 2. Login admin; Admin Console tab. 3. Leaderboard: explain formula and sources. 4. Award Badges, then Badges tab, change a threshold. 5. Moderation: New Report (user and a discussion post), Resolve, Dismiss, Block, Delete Reported Content. 6. Login as the blocked user -> blocked message. 7. Analytics. 8. Feedback status. 9. Show one invalid input.

## 10. Viva Q&A
1. Why a view? Score is derived data; a view is always current and stores nothing twice.
2. Where does score data come from? Members 2, 3, 4 real tables (see section 3).
3. How does it join the app? FeatureModule + one line in AppContext; no other module changed.
4. Why PreparedStatement? Stops SQL injection.
5. Why SwingWorker? DB calls must not run on the UI thread or the window freezes (consistent with M4's multithreading).
6. Why a transaction when blocking? Block and resolve must both succeed or both fail.
7. Why UNIQUE(user_id,badge_id)? One badge per user once.
8. How is a blocked user stopped? AuthService.login calls BlockCheck before Session.start.
9. Why does BlockCheck fail open? So Members 1-4's app still works if M5 tables are missing or MySQL is not used.
10. Why no FK on reports.target_id? M4's tables are intentionally FK-free; the author is still an FK via reported_id.
11. WHERE vs HAVING? WHERE filters rows before grouping, HAVING filters groups.
12. ON DELETE CASCADE? Deleting a user removes that user's rows automatically.
13. Limitation? Admin Console needs MySQL (in-memory mode shows a message). Event score counts RSVPs, not attendance. M4 answers use is_helpful set by Member 4's module.
14. Extension? New activity = new weight in v_impact and ImpactCalculator.

## 11. Report chapters
Introduction; Requirements; System design (architecture, flowchart, class and ER diagrams); Database design; Implementation (classes, screens, JDBC, SwingWorker); Impact score algorithm; Integration with Members 1-4; Testing; Limitations and future scope; Conclusion. Screenshots: Workbench tables, 5 tabs, validation error, blocked login, test output.
