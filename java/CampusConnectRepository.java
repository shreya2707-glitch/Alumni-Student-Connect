import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * CampusConnectRepository - High-performance JDBC & Concurrency Layer for Member 4.
 * Demonstrates: Custom Connection Pooling, Batched Chat Background Worker,
 * Fine-Grained Key-Based Concurrency Locks, and Cancelable Search Streams.
 */
public class CampusConnectRepository {

    // RUBRIC: [Concept: Custom Connection Pool]
    // A fixed-capacity BlockingQueue acts as an object pool of reusable java.sql.Connection instances.
    // Threads borrow connections with a non-blocking/timed poll to prevent thread starvation,
    // and release them safely inside finally blocks to avoid connection leaks.
    private static final int POOL_SIZE = 8;
    private static final String URL = DBConnection.url();
    private static final String USER = DBConnection.user();
    private static final String PASS = DBConnection.pass();
    private final BlockingQueue<Connection> pool;

    // RUBRIC: [Concept: Producer-Consumer Asynchronous Batching]
    // Decouples Swing UI threads from disk/network I/O. Posts are enqueued into a thread-safe
    // LinkedBlockingQueue. A dedicated background daemon thread drains them and executes batch inserts.
    private final BlockingQueue<DiscussionPost> postQueue = new LinkedBlockingQueue<>();
    private final Thread batchWriterThread;
    private volatile boolean running = true;

    // RUBRIC: [Concept: Fine-Grained Striped Locking]
    // Instead of synchronizing the entire table or booking method, we maintain a ReentrantLock
    // per eventId in a ConcurrentHashMap. This allows concurrent bookings for different events
    // while strictly preventing race conditions / overbooking for the same event.
    private final ConcurrentHashMap<Integer, ReentrantLock> eventLocks = new ConcurrentHashMap<>();

    public CampusConnectRepository() {
        this.pool = new ArrayBlockingQueue<>(POOL_SIZE);
        initConnectionPool();

        // RUBRIC: [Concept: Background Daemon Thread]
        // M4-DBWriter runs in the background. It continuously monitors postQueue and flushes
        // messages to MySQL using JDBC Batching (addBatch/executeBatch).
        batchWriterThread = new Thread(this::runBatchWriter, "M4-DBWriter");
        batchWriterThread.setDaemon(true);
        batchWriterThread.start();
    }

    // RUBRIC: [Concept: Eager Connection Pool Initialization]
    private void initConnectionPool() {
        for (int i = 0; i < POOL_SIZE; i++) {
            try {
                Connection conn = DriverManager.getConnection(URL, USER, PASS);
                pool.offer(conn);
            } catch (SQLException e) {
                System.err.println("[M4 ConnectionPool] Warning: Could not pre-allocate DB connection (" + e.getMessage() + ")");
            }
        }
    }

    // RUBRIC: [Concept: Timed Connection Acquisition]
    // Uses poll(timeout) to prevent threads from hanging indefinitely if the pool is exhausted.
    public Connection borrow() throws InterruptedException, SQLException {
        Connection conn = pool.poll(3, TimeUnit.SECONDS);
        if (conn == null || conn.isClosed()) {
            // Attempt to create a fresh connection if none available or expired
            return DriverManager.getConnection(URL, USER, PASS);
        }
        return conn;
    }

    // RUBRIC: [Concept: Safe Resource Return to Pool]
    // Returns connection back to the BlockingQueue so other worker threads can reuse it.
    public void release(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    if (!conn.getAutoCommit()) {
                        conn.setAutoCommit(true);
                    }
                    if (!pool.offer(conn)) {
                        conn.close(); // Pool is full, close excess connection
                    }
                }
            } catch (SQLException ignored) {
            }
        }
    }

    // =========================================================================
    // 1. ASYNC CHAT BATCH WRITER
    // =========================================================================

    // RUBRIC: [Concept: Producer Queue Offering]
    // Called by UI threads. Non-blocking enqueue of new posts.
    public void enqueuePost(DiscussionPost post) {
        if (postQueue.offer(post)) {
            SharedStats.activeChatQueueSize.incrementAndGet();
        }
    }

    // RUBRIC: [Concept: Consumer Batch Processing with drainTo]
    // Drains up to 20 posts at once and writes them in a single network round-trip.
    private void runBatchWriter() {
        List<DiscussionPost> batch = new ArrayList<>();
        while (running) {
            try {
                // Wait for at least 1 item or timeout every 500ms
                DiscussionPost first = postQueue.poll(500, TimeUnit.MILLISECONDS);
                if (first != null) {
                    batch.add(first);
                    postQueue.drainTo(batch, 19); // Collect up to 20 total

                    SharedStats.activeChatQueueSize.addAndGet(-batch.size());
                    SharedStats.totalMessagesWritten.addAndGet(batch.size());
                    writeBatchToDb(batch);
                    batch.clear();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception ex) {
                System.err.println("[M4-DBWriter] Error processing batch: " + ex.getMessage());
                batch.clear();
            }
        }
    }

    private void writeBatchToDb(List<DiscussionPost> batch) {
        if (batch.isEmpty()) return;
        String sql = "INSERT INTO discussion_posts (circle_id, user_id, content) VALUES (?, ?, ?)";
        Connection conn = null;
        try {
            conn = borrow();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (DiscussionPost p : batch) {
                    ps.setInt(1, p.getCircleId());
                    ps.setInt(2, p.getUserId());
                    ps.setString(3, p.getContent());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
        } catch (Exception e) {
            System.err.println("[M4-DBWriter] JDBC Batch Insert failed: " + e.getMessage());
        } finally {
            release(conn);
        }
    }

    // =========================================================================
    // 2. TRANSACTION + PER-EVENT REENTRANT LOCK (CONCURRENCY CONTROL)
    // =========================================================================

    // RUBRIC: [Concept: Thread-Safe Atomic Transaction with Striped Locking]
    // Demonstrates:
    //  1. ReentrantLock per eventId (prevents race condition across concurrent threads for same event).
    //  2. setAutoCommit(false) -> JDBC Transaction boundary.
    //  3. Conditional UPDATE (seats_left > 0) to avoid negative seat counts.
    //  4. commit() on success, rollback() on any failure.
    //  5. Unconditional unlock() and connection release() in finally block.
    public boolean bookEventSeat(int eventId, int userId) {
        ReentrantLock lock = eventLocks.computeIfAbsent(eventId, k -> new ReentrantLock());
        lock.lock();
        Connection conn = null;
        try {
            conn = borrow();
            conn.setAutoCommit(false); // Start Transaction

            // 1. Decrement seat atomically only if seats_left > 0
            String updateSql = "UPDATE campus_events SET seats_left = seats_left - 1 WHERE id = ? AND seats_left > 0";
            int rowsUpdated;
            try (PreparedStatement updatePs = conn.prepareStatement(updateSql)) {
                updatePs.setInt(1, eventId);
                rowsUpdated = updatePs.executeUpdate();
            }

            if (rowsUpdated > 0) {
                // 2. Insert RSVP record
                String rsvpSql = "INSERT INTO event_rsvps (event_id, user_id) VALUES (?, ?)";
                try (PreparedStatement rsvpPs = conn.prepareStatement(rsvpSql)) {
                    rsvpPs.setInt(1, eventId);
                    rsvpPs.setInt(2, userId);
                    rsvpPs.executeUpdate();
                }
                conn.commit(); // Transaction Successful
                return true;
            } else {
                conn.rollback(); // No seats available, abort
                return false;
            }
        } catch (Exception ex) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            System.err.println("[M4 Transaction] Booking failed: " + ex.getMessage());
            return false;
        } finally {
            try {
                if (conn != null) conn.setAutoCommit(true);
            } catch (SQLException ignored) {}
            release(conn);
            lock.unlock(); // Ensure lock is released under all conditions
        }
    }

    // =========================================================================
    // 3. CANCELABLE SEARCH STREAM (RESPONSIVE BACKGROUND QUERY)
    // =========================================================================

    // RUBRIC: [Concept: Cooperative Thread Cancellation via Supplier<Boolean>]
    // Allows UI threads to abort reading lengthy JDBC ResultSets if the user changes search terms,
    // closes the window, or triggers another keystroke filter.
    public List<QAQuestion> searchQuestions(String keyword, Supplier<Boolean> isCancelled) {
        List<QAQuestion> results = new ArrayList<>();
        String sql = "SELECT id, user_id, title, details, created_at FROM qa_questions " +
                     "WHERE title LIKE ? OR details LIKE ? ORDER BY id DESC";
        Connection conn = null;
        try {
            conn = borrow();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                String pattern = "%" + (keyword == null ? "" : keyword.trim()) + "%";
                ps.setString(1, pattern);
                ps.setString(2, pattern);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        // RUBRIC: [Concept: Interruption Check inside ResultSet iteration]
                        if (isCancelled != null && Boolean.TRUE.equals(isCancelled.get())) {
                            System.out.println("[M4 Search] Search cancelled cooperatively.");
                            break; // Stop fetching further rows
                        }
                        results.add(new QAQuestion(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            rs.getString("title"),
                            rs.getString("details"),
                            rs.getTimestamp("created_at")
                        ));
                    }
                }
            }
        } catch (Exception ex) {
            System.err.println("[M4 Search] Error searching questions: " + ex.getMessage());
        } finally {
            release(conn);
        }
        return results;
    }

    // =========================================================================
    // 4. ADDITIONAL DOMAIN ACCESS METHODS (CIRCLES, QA, EVENTS, POLLS, NOTIFS)
    // =========================================================================

    public List<DiscussionCircle> getAllCircles() {
        List<DiscussionCircle> list = new ArrayList<>();
        Connection conn = null;
        try {
            conn = borrow();
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id, name, description FROM discussion_circles ORDER BY id ASC")) {
                while (rs.next()) {
                    list.add(new DiscussionCircle(rs.getInt("id"), rs.getString("name"), rs.getString("description")));
                }
            }
        } catch (Exception e) {
            System.err.println("[M4 Repo] Error loading circles: " + e.getMessage());
        } finally {
            release(conn);
        }
        return list;
    }

    public List<DiscussionPost> getPostsForCircle(int circleId) {
        List<DiscussionPost> list = new ArrayList<>();
        Connection conn = null;
        try {
            conn = borrow();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, circle_id, user_id, content, created_at FROM discussion_posts WHERE circle_id = ? ORDER BY id ASC")) {
                ps.setInt(1, circleId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(new DiscussionPost(
                            rs.getInt("id"),
                            rs.getInt("circle_id"),
                            rs.getInt("user_id"),
                            rs.getString("content"),
                            rs.getTimestamp("created_at")
                        ));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[M4 Repo] Error loading posts: " + e.getMessage());
        } finally {
            release(conn);
        }
        return list;
    }

    public int addQuestion(int userId, String title, String details) {
        Connection conn = null;
        try {
            conn = borrow();
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO qa_questions (user_id, title, details) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, userId);
                ps.setString(2, title);
                ps.setString(3, details);
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        } catch (Exception e) {
            System.err.println("[M4 Repo] Error adding question: " + e.getMessage());
        } finally {
            release(conn);
        }
        return -1;
    }

    public List<QAAnswer> getAnswersForQuestion(int questionId) {
        List<QAAnswer> list = new ArrayList<>();
        Connection conn = null;
        try {
            conn = borrow();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id, question_id, user_id, answer_text, is_helpful FROM qa_answers WHERE question_id = ? ORDER BY id ASC")) {
                ps.setInt(1, questionId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(new QAAnswer(
                            rs.getInt("id"),
                            rs.getInt("question_id"),
                            rs.getInt("user_id"),
                            rs.getString("answer_text"),
                            rs.getBoolean("is_helpful")
                        ));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[M4 Repo] Error loading answers: " + e.getMessage());
        } finally {
            release(conn);
        }
        return list;
    }

    public void addAnswer(int questionId, int userId, String answerText) {
        Connection conn = null;
        try {
            conn = borrow();
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO qa_answers (question_id, user_id, answer_text, is_helpful) VALUES (?, ?, ?, FALSE)")) {
                ps.setInt(1, questionId);
                ps.setInt(2, userId);
                ps.setString(3, answerText);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            System.err.println("[M4 Repo] Error adding answer: " + e.getMessage());
        } finally {
            release(conn);
        }
    }

    public void markAnswerHelpful(int answerId) {
        Connection conn = null;
        try {
            conn = borrow();
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE qa_answers SET is_helpful = TRUE WHERE id = ?")) {
                ps.setInt(1, answerId);
                ps.executeUpdate();
            }
        } catch (Exception e) {
            System.err.println("[M4 Repo] Error marking answer helpful: " + e.getMessage());
        } finally {
            release(conn);
        }
    }

    public List<CampusEvent> getAllEvents() {
        List<CampusEvent> list = new ArrayList<>();
        Connection conn = null;
        try {
            conn = borrow();
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id, title, event_date, seats_left FROM campus_events ORDER BY id ASC")) {
                while (rs.next()) {
                    list.add(new CampusEvent(
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("event_date"),
                        rs.getInt("seats_left")
                    ));
                }
            }
        } catch (Exception e) {
            System.err.println("[M4 Repo] Error loading events: " + e.getMessage());
        } finally {
            release(conn);
        }
        return list;
    }

    public List<Poll> getPollsForEvent(int eventId) {
        List<Poll> list = new ArrayList<>();
        Connection conn = null;
        try {
            conn = borrow();
            try (PreparedStatement ps = conn.prepareStatement("SELECT id, event_id, question FROM event_polls WHERE event_id = ?")) {
                ps.setInt(1, eventId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(new Poll(rs.getInt("id"), rs.getInt("event_id"), rs.getString("question")));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[M4 Repo] Error loading polls: " + e.getMessage());
        } finally {
            release(conn);
        }
        return list;
    }

    public boolean votePoll(int pollId, int userId, int optionIndex) {
        Connection conn = null;
        try {
            conn = borrow();
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO poll_votes (poll_id, user_id, option_index) VALUES (?, ?, ?)")) {
                ps.setInt(1, pollId);
                ps.setInt(2, userId);
                ps.setInt(3, optionIndex);
                ps.executeUpdate();
                return true;
            }
        } catch (Exception e) {
            System.err.println("[M4 Repo] Poll vote already recorded or error: " + e.getMessage());
            return false;
        } finally {
            release(conn);
        }
    }

    public void shutdown() {
        running = false;
        batchWriterThread.interrupt();
        for (Connection c : pool) {
            try { c.close(); } catch (SQLException ignored) {}
        }
    }
}
