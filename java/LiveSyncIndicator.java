import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * LiveSyncIndicator - Custom Graphics2D component flashing green upon DB poll.
 * Visualizes the background NotificationPoller daemon thread's activity to the professor.
 */
public class LiveSyncIndicator extends JPanel {

    private boolean isFlashing = false;
    private final Timer checkTimer;
    private long lastSeenSync = 0;

    public LiveSyncIndicator() {
        setOpaque(false);
        setLayout(new FlowLayout(FlowLayout.RIGHT, 6, 2));

        JLabel label = new JLabel("Live DB Sync");
        label.setFont(new Font("SansSerif", Font.BOLD, 12));
        label.setForeground(new Color(220, 235, 255));

        // Circular beacon component
        JPanel beacon = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                if (isFlashing) {
                    // Bright flash
                    g2.setColor(Color.GREEN);
                    g2.fillOval(2, 2, 12, 12);
                    // Glow border
                    g2.setColor(new Color(150, 255, 150, 180));
                    g2.drawOval(0, 0, 15, 15);
                } else {
                    // Darker idle green
                    g2.setColor(new Color(0, 110, 0));
                    g2.fillOval(2, 2, 12, 12);
                    g2.setColor(new Color(0, 70, 0));
                    g2.drawOval(2, 2, 12, 12);
                }
                g2.dispose();
            }

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(16, 16);
            }
        };
        beacon.setOpaque(false);

        add(beacon);
        add(label);

        // Check for new sync every 100ms
        checkTimer = new Timer(100, (ActionEvent e) -> {
            long currentSync = SharedStats.lastSuccessfulDbSyncMillis.get();
            if (currentSync > lastSeenSync) {
                lastSeenSync = currentSync;
                isFlashing = true;
                beacon.repaint();

                // Fade back after 500ms
                Timer fadeTimer = new Timer(500, (ActionEvent fe) -> {
                    isFlashing = false;
                    beacon.repaint();
                });
                fadeTimer.setRepeats(false);
                fadeTimer.start();
            }
        });
        checkTimer.start();
    }

    public void stopTimer() {
        if (checkTimer != null) checkTimer.stop();
    }
}
