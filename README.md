# 🎓 AlumniConnect — Combined Alumni & Student Connect Platform

![Java](https://img.shields.io/badge/Java-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Java Swing](https://img.shields.io/badge/Java_Swing-GUI-blue?style=for-the-badge)
![MySQL](https://img.shields.io/badge/MySQL-005C84?style=for-the-badge&logo=mysql&logoColor=white)
![JDBC](https://img.shields.io/badge/JDBC-Database_Connectivity-green?style=for-the-badge)

**AlumniConnect** is a unified, full-featured desktop application built in Java (Swing + JDBC + MySQL) that bridges the gap between university students, alumni, and administrators. Designed with modular Object-Oriented Programming principles, a shared relational database architecture, and real-time multithreaded synchronization, the platform powers mentorship matching, career quests, job referrals, community events, and impact analytics.

---

## 🌟 Five Signature Features

1. **🤝 Mentor Match & Career Quest:** Algorithmic mentor-student pairing using customizable matching strategies (`MatchStrategy`, `CareerQuestMatcher`), paired with structured career roadmaps and milestone tracking.
2. **📊 Skill-Gap Analyzer & Job Referrals:** Interactive job and internship board allowing students to compare their current skill sets against industry requirements (`SkillMatcher`) and request direct alumni referrals.
3. **💬 Ask an Alumnus & Discussion Circles:** Real-time Q&A boards, topic-based discussion forums, campus event calendars, and background notification polling threads.
4. **🏆 Community Impact Leaderboard & Badges:** Gamified recognition engine (`ImpactCalculator`) that tracks completed mentorships, referrals, and answered questions to award dynamic achievement badges.
5. **🛡️ Admin & Moderation Studio:** Centralized dashboard featuring live thread monitors, user moderation workflows, system-wide analytics, and fallback in-memory/JDBC repository switching.

---

## 👥 Team & Module Ownership

Our architecture connects five specialized modules over a shared `AppContext` and MySQL database contract (`Users · Profiles · Jobs · Mentorships · Events · Messages · Achievements`):

| Module | Developer | PRN | Core Responsibilities & Key Classes |
| :--- | :--- | :--- | :--- |
| **Module 1: Core Auth, Profiles & Discovery** | **Shreeya Alwar** | `25070122180` | Authentication (`AuthService`, `PasswordUtil`), `User` role inheritance (`Student`, `Alumni`, `Admin`), profile foundations (`ProfilePanel`), and alumni directory discovery (`DiscoveryMatcher`, `AlumniCard`). |
| **Module 2: Mentorship & Career Quests** | **Shreya Panda** | `25070122181` | Matching engine (`MatchEngine`, `MatchStrategy`, `CareerQuestMatcher`), mentorship request workflows (`MentorshipModule`, `MentorshipPanel`), career roadmaps, and progress tracking. |
| **Module 3: Jobs, Applications & Referrals** | **Saurav Paudel** | `25070122174` | Job and internship listings (`JobBoardModule`, `JobBoardPanel`), application workflows (`ApplicationRepository`), skill-gap analysis (`SkillMatcher`), and referral management (`ReferralRepository`). |
| **Module 4: Community, Q&A & Events** | **Sourabh Gupta** | `25070122191` | Discussion circles (`DiscussionCirclesPanel`), Ask an Alumnus (`QABoardPanel`), campus events (`EventsCalendarPanel`), interactive quizzes (`QuizPanel`), and background notification threads (`NotificationPoller`, `LiveSyncIndicator`). |
| **Module 5: Impact, Analytics & Admin Studio** | **Sneha Sah** | `25070122189` | Gamified achievements & badges (`BadgePanel`, `ImpactCalculator`), `LeaderboardPanel`, content moderation (`ModerationPanel`), system analytics (`AnalyticsPanel`, `ThreadMonitorPanel`), and full-system integration (`MainFrame`). |

---

## 🧩 Core Java Syllabus Implementation Matrix

Every module adheres to shared interfaces and OOP contracts while highlighting core Java concepts across the codebase:

| Java Concept | Primary Lead | Supporting Modules & Implementation Details |
| :--- | :--- | :--- |
| **OOP & Inheritance** | Shreeya Alwar (M1) | All modules utilize the base `User` class and role-specific subclasses (`Student`, `Alumni`, `Admin`), alongside interface contracts (`FeatureModule`, `MatchStrategy`, `UserRepository`). |
| **Collections Framework** | Shreya Panda (M2) | Extensive use of `List`, `Map`, `Set`, and custom comparators for mentor matching scores, skill-gap filtering, discussion feeds, and leaderboard sorting across M2, M3, M4, and M5. |
| **Exception Handling** | Shreeya Alwar (M1) | Custom checked/unchecked exceptions (`AuthException`), input validation (`Validator`), and graceful SQL/IO error recovery across all modules. |
| **JDBC & CRUD Operations** | Saurav Paudel (M3) | Shared `DBConnection` pool managing MySQL CRUD operations across `JdbcUserRepository`, `MentorshipRepository`, `JobRepository`, `CampusConnectRepository`, and `ImpactRepository`. |
| **Java Swing GUI** | Sneha Sah (M5) | Unified dark/modern custom UI theme (`Ui.java`) integrating all five feature screens into `MainFrame` with custom panels, tables, and dialogs. |
| **Multithreading** | Sourabh Gupta (M4) | Asynchronous background polling (`NotificationPoller`, `LiveSyncIndicator`) for live community updates, integrated and monitored via M5's `ThreadMonitorPanel`. |
| **Design & Analysis** | All 5 Members | Modular repository pattern, database schema normalization (`sql/01` to `sql/06`), integration documentation (`docs/`), and automated verification tests (`M5Tests.java`). |

---

## 📂 Project Directory Structure

```text
AlumniConnect/
├── docs/                       # Architecture contracts & module integration guides
│   ├── CONTRACT.md
│   ├── INTEGRATION_STEPS.md
│   ├── INTEGRATION_M2.txt & M3.txt
│   └── README_M4.md & M5_DOCUMENTATION.md
├── java/                       # Complete Java source files (M1–M5)
│   ├── Main.java               # Application entry point
│   ├── MainFrame.java          # Master Swing container & navigation
│   ├── DBConnection.java       # Shared JDBC MySQL connection manager
│   └── ...                     # Domain models, repositories, matchers & UI panels
├── sql/                        # Sequential MySQL database schemas & seed data
│   ├── 01_schema_M1.sql        # Users & Profiles schema
│   ├── 02_M2_SCHEMA.sql        # Mentorships & Career Quests schema
│   ├── 03_M3_SCHEMA.sql        # Jobs, Applications & Referrals schema
│   ├── 04_M4_SCHEMA.sql        # Discussions, Q&A & Events schema
│   ├── 05_M5_SCHEMA.sql        # Badges, Reports & Impact schema
│   ├── 06_M5_DEMO_DATA.sql     # Rich sample dataset for viva demonstrations
│   └── M5_QUERIES.sql          # Analytical & verification SQL queries
├── AlumniNetwork.jar           # Pre-built executable JAR
├── mysql-connector.jar         # MySQL JDBC Driver
├── run.bat                     # One-click build & launch script for Windows
└── run.sh                      # One-click build & launch script for macOS/Linux
```

---

## 🚀 Getting Started & Setup Instructions

### Prerequisites
* **Java Development Kit (JDK):** JDK 17 or higher
* **MySQL Server:** MySQL 8.0+ (optional if running in offline/in-memory fallback mode, recommended for full JDBC persistence)

### 1. Clone the Repository
```bash
git clone [https://github.com/shreya2707-glitch/Alumni-Student-Connect.git](https://github.com/shreya2707-glitch/Alumni-Student-Connect.git)
cd Alumni-Student-Connect
```

### 2. Configure the MySQL Database
Run the SQL scripts inside the `sql/` folder in sequential order (`01` through `06`) using MySQL Workbench or the MySQL CLI:
```sql
SOURCE sql/01_schema_M1.sql;
SOURCE sql/02_M2_SCHEMA.sql;
SOURCE sql/03_M3_SCHEMA.sql;
SOURCE sql/04_M4_SCHEMA.sql;
SOURCE sql/05_M5_SCHEMA.sql;
SOURCE sql/06_M5_DEMO_DATA.sql;
```
*(Ensure your database credentials in `java/DBConnection.java` match your local MySQL root/user password).*

### 3. Build & Run the Application

**On Windows (PowerShell / CMD):**
```powershell
.\run.bat
```

**On Linux / macOS:**
```bash
chmod +x run.sh
./run.sh
```

**Or run the pre-compiled JAR directly:**
```bash
java -cp "AlumniNetwork.jar;mysql-connector.jar" Main
```
