# Member 4: Campus Connect & Live Activities Module

## Overview
This folder contains the complete, isolated implementation for **Member 4 (Campus Connect & Live Activities)** for the *Alumni–Student Connect Platform*.

It is developed in **Core Java 17+ SE** using standard `java.sql.*`, `javax.swing.*`, and `java.util.concurrent.*` with zero external dependencies (no Maven, Lombok, Spring, or HikariCP).

---

## 🛠️ Concurrency & Multithreading Features (Viva Rubrics)
1. **Custom Connection Pool (`CampusConnectRepository.java`)**: Pre-allocated `ArrayBlockingQueue<Connection>` (capacity 8) with timed acquisition (`poll(3, TimeUnit.SECONDS)`) and safe `release()` pattern.
2. **Producer-Consumer Async Chat Batch Writer (`M4-DBWriter`)**: Swing UI threads enqueue posts to a `LinkedBlockingQueue<DiscussionPost>`; a background daemon thread drains up to 20 messages with `drainTo()` and executes JDBC batch inserts (`executeBatch()`).
3. **Transactional Striped ReentrantLock (`bookEventSeat`)**: Fine-grained locking per `eventId` using `ConcurrentHashMap<Integer, ReentrantLock>` combined with an atomic SQL decrement transaction (`setAutoCommit(false)`, `commit()`, `rollback()`).
4. **Cooperative Debounced Search Worker (`QABoardPanel.java`)**: 300ms debounce using `javax.swing.Timer`; cancels stale background queries (`worker.cancel(true)`); leverages `SwingWorker<Void, QAQuestion>` with `publish()` / `process()`.
5. **Background Delta Reader (`M4-ChatReader`)**: Daemon thread polling MySQL every 1.5s for new messages, safely marshaling UI updates via `SwingUtilities.invokeLater()`.
6. **Parallel Startup Initialization (`CampusConnectPanel.java`)**: Spawns an `ExecutorService` (size 3) + `CountDownLatch(3)` to fetch initial datasets concurrently before building UI tabs.
7. **Resilient Notification Poller (`NotificationPoller.java`)**: Uses `ScheduledExecutorService` with `scheduleWithFixedDelay(10s)` and robust `try/catch` wrapping.
8. **Live Concurrency Diagnostics (`ThreadMonitorPanel.java`)**: Displays live `Thread.activeCount()`, atomic chat queue size, and volatile polling heartbeat.
9. **Viva Concurrency Stress Test Demo (`EventsCalendarPanel.java`)**: 100-thread race condition demonstration pitting an unsynchronized `int` against an `AtomicInteger` using a synchronized `CountDownLatch` starting gate.

---

## 🚀 How to Compile and Run

### Step 1: Database Setup (MySQL)
Make sure MySQL is running on `localhost:3306` (default user: `root`, password: `""`). Run the isolated Member 4 SQL script:
```sql
mysql -u root < member4_schema.sql
```
*(Or import `member4_schema.sql` via MySQL Workbench / phpMyAdmin into the `alumni_db` database).*

---

### Step 2: Compile the Code
Open PowerShell or Command Prompt in `Member_4_Work`:
```powershell
cd C:\Users\HP\Documents\allumniDiscovery_all_java\Member_4_Work
javac *.java
```

---

### Step 3: Run the Standalone Launcher
Run the application standalone (without needing Member 1's login screen):
```powershell
java Member4Launcher
```
*A prompt will appear allowing you to test as either **Student (User 1)** or **Alumni (User 2)**.*

---

## 🔌 1-Line Final Integration with Main Dashboard
To integrate into the main platform:
1. Copy or reference `CampusConnectModule.java` and its dependencies.
2. In `AppContext.init()`, simply register the module:
```java
modules.add(new CampusConnectModule());
```
The tab `"Campus Connect & Activities"` will automatically appear in `MainFrame` after login for both students and alumni.
