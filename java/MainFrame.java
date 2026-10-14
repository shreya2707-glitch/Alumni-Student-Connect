import javax.swing.*;
import java.awt.*;
import java.util.*;

public class MainFrame extends JFrame {
    final User user;
    Student profile;                       // null unless the user is a student
    ArrayList<Alumni> all;
    JTabbedPane tabs = new JTabbedPane();
    QuizPanel quiz;
    DashboardPanel dash;
    NetworkMapPanel map;
    ComparePanel compare;
    ProfilePanel profilePanel;

    public MainFrame(User user) {
        super("Smart Alumni Network - " + user.getFullName() + " (" + user.getType() + ")");
        this.user = user;
        this.profile = user instanceof Student ? (Student) user : null;
        this.all = new ArrayList<>(AppContext.alumni.findAllAlumni());

        dash = new DashboardPanel(this);
        map = new NetworkMapPanel(this);
        compare = new ComparePanel(this);
        profilePanel = new ProfilePanel(this, user);

        if (profile != null) { quiz = new QuizPanel(this, profile); tabs.addTab("Career DNA Quiz", quiz); }
        tabs.addTab("Discovery Dashboard", dash);
        tabs.addTab("Network Map", map);
        tabs.addTab("Compare Profiles", compare);
        tabs.addTab("My Profile", profilePanel);
        for (FeatureModule m : AppContext.modules)               // other members' screens
            if (m.isVisibleFor(user.getType())) tabs.addTab(m.getTitle(), m.createPanel(user));

        JPanel bar = new JPanel(new BorderLayout());
        bar.add(new JLabel("  " + user.getWelcomeMessage()), BorderLayout.WEST);
        JButton logout = new JButton("Logout");
        logout.addActionListener(e -> {
            AppContext.auth.logout();
            dispose();
            new LoginFrame().setVisible(true);
        });
        bar.add(logout, BorderLayout.EAST);

        JLabel status = new JLabel("  Data source: " + (AppContext.usingDatabase ? "MySQL (JDBC)" : "in-memory sample data")
            + "  |  " + all.size() + " alumni");
        add(bar, BorderLayout.NORTH);
        add(tabs, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1050, 700);
        setLocationRelativeTo(null);
    }

    public boolean hasMatchData() { return profile != null && !profile.getSkills().isEmpty(); }

    public int scoreOf(Alumni a) { return hasMatchData() ? AppContext.matcher.score(profile, a) : 0; }

    public void goTo(Component c) { tabs.setSelectedComponent(c); }

    /** Called after the profile or quiz changes. */
    public void onProfileChanged() {
        all = new ArrayList<>(AppContext.alumni.findAllAlumni());
        dash.refresh();
        map.repaint();
        compare.update();
    }

    /** Signature feature: Find My Career Mentor. */
    public void findMentor() {
        if (profile == null) {
            JOptionPane.showMessageDialog(this, "Mentor matching is available for student accounts.");
            return;
        }
        if (!hasMatchData()) {
            JOptionPane.showMessageDialog(this, "Take the Career DNA quiz first so I can match you.");
            goTo(quiz);
            return;
        }
        ArrayList<Alumni> ranked = MatchEngine.rank(profile, all, AppContext.matcher);
        if (ranked.isEmpty()) return;
        Alumni top = ranked.get(0);
        StringBuilder sb = new StringBuilder();
        sb.append("YOUR BEST MENTOR\n\n");
        sb.append(top.getName()).append(" - ").append(top.getSummary()).append("\n");
        sb.append("Match score: ").append(scoreOf(top)).append("%\n\n");
        sb.append("Why: shared skills -> ").append(MatchEngine.shared(profile.getSkills(), top.getSkills())).append("\n");
        sb.append("     shared interests -> ").append(MatchEngine.shared(profile.getInterests(), top.getInterests())).append("\n");
        if (top.getIndustry().equalsIgnoreCase(profile.getTargetIndustry()))
            sb.append("     works in your target industry\n");
        if (top.getGoal().equalsIgnoreCase(profile.getGoal()))
            sb.append("     same career goal: ").append(top.getGoal()).append("\n");
        sb.append("\nCareer journey: ").append(top.getJourney()).append("\n\nRunners-up:\n");
        for (int i = 1; i < Math.min(3, ranked.size()); i++)
            sb.append("  ").append(i + 1).append(". ").append(ranked.get(i).getName())
              .append(" (").append(scoreOf(ranked.get(i))).append("%)\n");
        JTextArea ta = new JTextArea(sb.toString());
        ta.setEditable(false); ta.setFont(new Font("SansSerif", Font.PLAIN, 14));
        JOptionPane.showMessageDialog(this, ta, "Find My Career Mentor", JOptionPane.INFORMATION_MESSAGE);
    }
}
