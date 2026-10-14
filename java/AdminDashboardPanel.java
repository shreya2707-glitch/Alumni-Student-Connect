import javax.swing.*;
import java.awt.*;

/** Member 5: Admin Dashboard = five tabs. */
public class AdminDashboardPanel extends JPanel {
    public AdminDashboardPanel(User admin) {
        setLayout(new BorderLayout(6, 6));
        JLabel head = new JLabel("  Admin Console  |  " + admin.getFullName());
        head.setFont(new Font("SansSerif", Font.BOLD, 17));
        JTabbedPane tabs = new JTabbedPane();
        LeaderboardPanel lb = new LeaderboardPanel();
        BadgePanel badges = new BadgePanel();
        ModerationPanel mod = new ModerationPanel(admin);
        AnalyticsPanel an = new AnalyticsPanel();
        FeedbackPanel fb = new FeedbackPanel();
        tabs.addTab("Leaderboard", lb);
        tabs.addTab("Badges", badges);
        tabs.addTab("Moderation", mod);
        tabs.addTab("Analytics", an);
        tabs.addTab("Feedback", fb);
        tabs.addChangeListener(e -> {                       // always show fresh data when a tab opens
            Component c = tabs.getSelectedComponent();
            if (c == lb) lb.load(); else if (c == badges) badges.load(); else if (c == mod) mod.load();
            else if (c == an) an.load(); else if (c == fb) fb.load();
        });
        add(head, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
    }
}
