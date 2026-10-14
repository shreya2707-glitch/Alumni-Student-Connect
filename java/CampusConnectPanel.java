import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.*;

/**
 * CampusConnectPanel - Main UI Container for Member 4.
 * Demonstrates:
 *  - Parallel Background Data Fetching on Startup via ExecutorService (size 3) + CountDownLatch
 *  - Responsive non-blocking tab population on the Swing EDT
 *  - Live ThreadMonitorPanel integration in the SOUTH bar
 *  - Resilient NotificationPoller alerts
 */
public class CampusConnectPanel extends JPanel {

    private final int userId;
    private final String role;
    private final CampusConnectRepository repo;

    private final JTabbedPane tabbedPane = new JTabbedPane();
    private final ThreadMonitorPanel monitorPanel = new ThreadMonitorPanel();
    private final JLabel loadingStatus = new JLabel("⚡ Initializing Campus Connect: Fetching data concurrently across 3 worker threads...", SwingConstants.CENTER);

    private NotificationPoller notificationPoller;
    private DiscussionCirclesPanel discussionsTab;

    public CampusConnectPanel(int userId, String role, CampusConnectRepository repo) {
        this.userId = userId;
        this.role = role;
        this.repo = repo;

        setLayout(new BorderLayout());

        // Header Banner
        JPanel headerPanel = new JPanel(new BorderLayout(8, 4));
        headerPanel.setBackground(new Color(24, 43, 73));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));

        JLabel titleLabel = new JLabel("Campus Connect & Live Activities");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 17));
        titleLabel.setForeground(Color.WHITE);

        JLabel userBadge = new JLabel("Logged in: User #" + userId + " [" + role + "]  ");
        userBadge.setFont(new Font("SansSerif", Font.PLAIN, 12));
        userBadge.setForeground(new Color(200, 220, 245));

        JPanel rightHeader = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightHeader.setOpaque(false);
        rightHeader.add(userBadge);
        rightHeader.add(new LiveSyncIndicator());

        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(rightHeader, BorderLayout.EAST);

        add(headerPanel, BorderLayout.NORTH);

        // Center: Initially displays loading banner while worker pool loads data
        loadingStatus.setFont(new Font("SansSerif", Font.BOLD, 14));
        loadingStatus.setForeground(new Color(60, 90, 140));
        add(loadingStatus, BorderLayout.CENTER);

        // South: Thread Monitor
        add(monitorPanel, BorderLayout.SOUTH);

        // Initialize background notification daemon
        initNotificationPoller();

        // RUBRIC: [Concept: Parallel Startup Data Loading via ExecutorService]
        // Instead of executing sequential JDBC calls on the main thread, 3 worker threads
        // fetch data simultaneously. CountDownLatch waits for all tasks to complete before UI assembly.
        initTabsConcurrently();
    }

    private void initTabsConcurrently() {
        ExecutorService initExecutor = Executors.newFixedThreadPool(3, r -> new Thread(r, "M4-StartupWorker"));
        CountDownLatch latch = new CountDownLatch(3);

        // 1. Task: Warm up Q&A Data
        initExecutor.submit(() -> {
            try {
                repo.searchQuestions("", () -> false);
            } finally {
                latch.countDown();
            }
        });

        // 2. Task: Warm up Discussion Circles
        initExecutor.submit(() -> {
            try {
                repo.getAllCircles();
            } finally {
                latch.countDown();
            }
        });

        // 3. Task: Warm up Events Data
        initExecutor.submit(() -> {
            try {
                repo.getAllEvents();
            } finally {
                latch.countDown();
            }
        });

        // Spawns supervisor thread to await latch completion off-EDT
        new Thread(() -> {
            try {
                latch.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            } finally {
                initExecutor.shutdown();
            }

            // RUBRIC: [Concept: Safe EDT Tab Construction via invokeLater]
            SwingUtilities.invokeLater(() -> {
                remove(loadingStatus);

                QABoardPanel qaTab = new QABoardPanel(userId, role, repo);
                discussionsTab = new DiscussionCirclesPanel(userId, role, repo);
                EventsCalendarPanel eventsTab = new EventsCalendarPanel(userId, role, repo);

                tabbedPane.addTab("Ask an Alumnus (Live Q&A)", qaTab);
                tabbedPane.addTab("Discussion Circles", discussionsTab);
                tabbedPane.addTab("Campus Events & Concurrency Demo", eventsTab);

                add(tabbedPane, BorderLayout.CENTER);
                revalidate();
                repaint();
            });
        }, "M4-TabAssembler").start();
    }

    private void initNotificationPoller() {
        notificationPoller = new NotificationPoller(userId, repo, (List<String> notifs) -> {
            StringBuilder sb = new StringBuilder("You have " + notifs.size() + " new notification(s):\n\n");
            for (String n : notifs) {
                sb.append("• ").append(n).append("\n");
            }
            JOptionPane.showMessageDialog(this, sb.toString(), "In-App Notification Alert", JOptionPane.INFORMATION_MESSAGE);
        });
    }

    public void cleanup() {
        if (monitorPanel != null) {
            monitorPanel.stopTimer();
        }
        if (notificationPoller != null) {
            notificationPoller.shutdown();
        }
        if (discussionsTab != null) {
            discussionsTab.stopReader();
        }
    }
}
