# AlumniConnect — Combined Alumni–Student Connect Platform

All five members' modules in one Java Swing app.

```
AlumniConnect/
├── java/      merged source (flat folder). AppContext registers every module
├── sql/       MySQL scripts, run in order 01 -> 06
├── docs/      each member's original integration notes
├── run.bat / run.sh     compile + launch
└── mysql-connector.jar, AlumniNetwork.jar
```

## Run (double-click run.bat, or ./run.sh)
Works immediately with **no database** (in-memory sample data):
Mentorship, Opportunity Arena and the Admin Console open; Campus Connect needs MySQL for live data.

Logins: student1 / student123 · aarav / alumni123 · admin / admin123
Admin sees the **Admin Console** tab (Leaderboard, Badges, Moderation, Analytics, Feedback).

## Run with MySQL (full features)
1. Run sql/01 ... sql/06 in order (06 is optional demo data; start the app once first so users exist).
2. Put your MySQL password in `java/DBConnection.java` (`PASS`). Member 4 now reads the same credentials, so there is only one place to set it.
3. Run run.bat / run.sh.

## What changed when combining
- `AppContext.java`: registers MentorshipModule, JobBoardModule, CampusConnectModule, AdminModule.
- `AuthService.java`: Member 5's version (blocked users cannot log in).
- `DBConnection.java`: added url()/user()/pass() getters; `CampusConnectRepository.java` uses them instead of its own hard-coded password.
- Everything else is unchanged. Member 4's duplicate User/Role/FeatureModule were identical to Member 1's, so one copy is kept.
