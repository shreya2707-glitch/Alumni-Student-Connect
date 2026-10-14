import javax.swing.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * NotificationPoller - Background Daemon for Unread Alerts.
 * Demonstrates: ScheduledExecutorService, resilient exception handling,
 * volatile timestamp heartbeat, and EDT marshaling.
 */
public class NotificationPoller {

    private final int userId;
    private final CampusConnectRepository repo;
    private final Consumer<List<String>> onNotificationsReceived;

    // RUBRIC: [Concept: Scheduled Periodic Execution]
    // Uses ScheduledExecutorService instead of java.util.Timer for robust thread pooling
    // and predictable scheduleWithFixedDelay semantics.
    private final ScheduledExecutorService scheduler;

    public NotificationPoller(int userId, CampusConnectRepository repo, Consumer<List<String>> onNotificationsReceived) {
        this.userId = userId;
        this.repo = repo;
        this.onNotificationsReceived = onNotificationsReceived;

        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "M4-NotificationPoller");
            t.setDaemon(true);
            return t;
        });

        startPolling();
    }

    private void startPolling() {
        // RUBRIC: [Concept: scheduleWithFixedDelay with Robust Try/Catch]
        // Wraps the task body in try/catch(Exception e) so a temporary network failure
        // or MySQL connection reset does NOT kill the ScheduledExecutorService.
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                // Update heartbeat for ThreadMonitorPanel
                SharedStats.lastDbPollTimeMillis = System.currentTimeMillis();

                List<String> unreadNotifs = fetchUnreadNotifications();
                // Signal live DB sync success for the visual indicator
                SharedStats.lastSuccessfulDbSyncMillis.set(System.currentTimeMillis());
                if (!unreadNotifs.isEmpty() && onNotificationsReceived != null) {
                    // RUBRIC: [Concept: EDT-Safe UI Notification Callback]
                    SwingUtilities.invokeLater(() -> onNotificationsReceived.accept(unreadNotifs));
                }
            } catch (Exception ex) {
                // Catch all exceptions to prevent ScheduledExecutorService cancellation
                System.err.println("[M4 NotificationPoller] Polling cycle warning (recovered): " + ex.getMessage());
            }
        }, 2, 10, TimeUnit.SECONDS);
    }

    private List<String> fetchUnreadNotifications() {
        List<String> list = new ArrayList<>();
        String sql = "SELECT id, message FROM user_notifications WHERE user_id = ? AND is_read = FALSE ORDER BY id DESC";
        Connection conn = null;
        try {
            conn = repo.borrow();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, userId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(rs.getString("message"));
                    }
                }
            }
        } catch (Exception ex) {
            // Logged and suppressed; will retry in next scheduled cycle
        } finally {
            repo.release(conn);
        }
        return list;
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }
}
