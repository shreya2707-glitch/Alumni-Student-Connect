import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * ThreadMonitorPanel - Live diagnostics dashboard showing multithreading runtime statistics.
 * Demonstrates periodic sampling of JVM active thread count, queue buffer size,
 * and background DB polling heartbeat.
 */
public class ThreadMonitorPanel extends JPanel {

    private final JLabel jvmThreadsLabel = new JLabel();
    private final JLabel chatQueueLabel = new JLabel();
    private final JLabel lastPollLabel = new JLabel();

    // RUBRIC: [Concept: Periodic UI Polling via javax.swing.Timer]
    // Runs on the Event Dispatch Thread (EDT) every 500ms, reading non-blocking thread stats.
    private final Timer pollTimer;

    public ThreadMonitorPanel() {
        setLayout(new FlowLayout(FlowLayout.LEFT, 20, 6));
        setBackground(new Color(238, 242, 246));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(200, 208, 216)),
                BorderFactory.createEmptyBorder(2, 10, 2, 10)
        ));

        Font statFont = new Font("Monospaced", Font.BOLD, 12);
        jvmThreadsLabel.setFont(statFont);
        chatQueueLabel.setFont(statFont);
        lastPollLabel.setFont(statFont);

        add(jvmThreadsLabel);
        add(new JSeparator(SwingConstants.VERTICAL));
        add(chatQueueLabel);
        add(new JSeparator(SwingConstants.VERTICAL));
        add(lastPollLabel);

        updateLabels();

        // 500ms update interval
        pollTimer = new Timer(500, (ActionEvent e) -> updateLabels());
        pollTimer.start();
    }

    private void updateLabels() {
        // Read Thread.activeCount() directly from JVM
        int activeThreads = Thread.activeCount();
        jvmThreadsLabel.setText("JVM Threads: " + activeThreads);

        // Read thread-safe atomic counter
        int queueSize = SharedStats.activeChatQueueSize.get();
        chatQueueLabel.setText("Chat Queue Size: " + queueSize);

        // Read volatile timestamp
        long secondsSincePoll = (System.currentTimeMillis() - SharedStats.lastDbPollTimeMillis) / 1000;
        lastPollLabel.setText("Last DB Poll: " + secondsSincePoll + "s ago");
    }

    public void stopTimer() {
        if (pollTimer != null && pollTimer.isRunning()) {
            pollTimer.stop();
        }
    }
}
