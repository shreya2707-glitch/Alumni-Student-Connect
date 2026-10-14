import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/** Member 5: leaderboard. Data is read from Members 2, 3 and 4's tables through the v_impact view. */
public class LeaderboardPanel extends JPanel {
    private final ImpactRepository repo = new ImpactRepository();
    private final DefaultTableModel model = Ui.model("Rank", "Name", "Role", "Mentorship x20 (M2)", "Answers x10 (M4)",
            "Referrals x15 (M3)", "Events x5 (M4)", "Score", "Badge");

    public LeaderboardPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JButton refresh = new JButton("Refresh"), award = new JButton("Award Badges");
        refresh.addActionListener(e -> load());
        award.addActionListener(e -> Ui.async(repo::awardBadges, n -> { Ui.info(this, n + " new achievement(s) awarded."); load(); }, this));
        add(new JLabel("Score = Mentorship x20 + Helpful Answer x10 + Referral x15 + Event x5   (live from Members 2, 3, 4)"), BorderLayout.NORTH);
        add(new JScrollPane(Ui.table(model)), BorderLayout.CENTER);
        add(Ui.buttons(refresh, award), BorderLayout.SOUTH);
        load();
    }

    @SuppressWarnings("unchecked")
    public void load() {
        Ui.async(() -> new Object[]{repo.badges(), repo.leaderboard()}, d -> {
            List<Badge> badges = (List<Badge>) d[0];
            model.setRowCount(0);
            int rank = 1;
            for (CommunityImpact c : (List<CommunityImpact>) d[1])
                model.addRow(new Object[]{rank++, c.getName(), c.getRole(), c.getMentorships(), c.getHelpfulAnswers(),
                        c.getReferrals(), c.getEvents(), c.getTotal(), ImpactCalculator.badgeFor(c.getTotal(), badges)});
        }, this);
    }
}
