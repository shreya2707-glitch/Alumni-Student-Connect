import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/** Discovery dashboard: filtering, sorting, explore career paths, cards. */
public class DashboardPanel extends JPanel {
    private final MainFrame frame;
    private final JComboBox<String> sortBox = new JComboBox<>(
        new String[]{"Match score (high to low)", "Graduation year (newest)", "Name (A-Z)"});
    private final JComboBox<String> exploreType = new JComboBox<>(new String[]{"All", "Company", "Role", "Industry"});
    private final JComboBox<String> exploreValue = new JComboBox<>();
    private final JTextField search = new JTextField(12);
    private final JPanel cards = new JPanel(new GridLayout(0, 2, 10, 10));
    private final JLabel count = new JLabel();
    private boolean updating = false;

    public DashboardPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        controls.add(new JLabel("Explore by:")); controls.add(exploreType); controls.add(exploreValue);
        controls.add(new JLabel("Search:")); controls.add(search);
        controls.add(new JLabel("Sort:")); controls.add(sortBox);
        JButton mentor = new JButton("\u2605 Find My Career Mentor");
        mentor.setFont(new Font("SansSerif", Font.BOLD, 14));
        mentor.setBackground(new Color(255, 214, 90));
        mentor.addActionListener(e -> frame.findMentor());
        controls.add(mentor);
        add(controls, BorderLayout.NORTH);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.add(cards, BorderLayout.NORTH);
        add(new JScrollPane(wrap), BorderLayout.CENTER);
        add(count, BorderLayout.SOUTH);

        exploreType.addActionListener(e -> { fillExploreValues(); refresh(); });
        exploreValue.addActionListener(e -> refresh());
        sortBox.addActionListener(e -> refresh());
        search.addActionListener(e -> refresh());
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { refresh(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { refresh(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { refresh(); }
        });
        fillExploreValues();
        refresh();
    }

    private void fillExploreValues() {
        updating = true;
        exploreValue.removeAllItems();
        String type = (String) exploreType.getSelectedItem();
        TreeSet<String> vals = new TreeSet<>();
        for (Alumni a : frame.all) {
            if ("Company".equals(type)) vals.add(a.getCompany());
            else if ("Role".equals(type)) vals.add(a.getJobRole());
            else if ("Industry".equals(type)) vals.add(a.getIndustry());
        }
        for (String v : vals) exploreValue.addItem(v);
        exploreValue.setEnabled(!"All".equals(type));
        updating = false;
    }

    public void refresh() {
        if (updating) return;
        String type = (String) exploreType.getSelectedItem();
        String val = (String) exploreValue.getSelectedItem();
        String q = search.getText().trim().toLowerCase();

        // FILTERING
        List<Alumni> list = frame.all.stream().filter(a -> {
            if (val != null && !"All".equals(type)) {
                if ("Company".equals(type) && !a.getCompany().equals(val)) return false;
                if ("Role".equals(type) && !a.getJobRole().equals(val)) return false;
                if ("Industry".equals(type) && !a.getIndustry().equals(val)) return false;
            }
            if (!q.isEmpty()) {
                String hay = (a.getName() + a.getCompany() + a.getJobRole() + a.getSkills()).toLowerCase();
                if (!hay.contains(q)) return false;
            }
            return true;
        }).collect(Collectors.toCollection(ArrayList::new));

        // SORTING
        switch (sortBox.getSelectedIndex()) {
            case 0: list.sort((x, y) -> Integer.compare(frame.scoreOf(y), frame.scoreOf(x))); break;
            case 1: list.sort((x, y) -> Integer.compare(y.getGradYear(), x.getGradYear())); break;
            default: list.sort(Comparator.comparing(Alumni::getName));
        }

        cards.removeAll();
        for (Alumni a : list) {
            cards.add(new AlumniCard(a, frame.scoreOf(a), frame.hasMatchData(),
                () -> frame.compare.pick(a), () -> showDetails(a)));
        }
        count.setText("  Showing " + list.size() + " of " + frame.all.size() + " alumni");
        cards.revalidate(); cards.repaint();
    }

    public static void showDetails(Alumni a) {
        String s = a.getName() + "\n" + a.getSummary() + "\n\nQualification: " + a.getQualification() +
            "\nSkills: " + String.join(", ", a.getSkills()) +
            "\nInterests: " + String.join(", ", a.getInterests()) +
            "\nGoal: " + a.getGoal() + "\n\nJourney: " + a.getJourney();
        JOptionPane.showMessageDialog(null, s, "Alumni Profile", JOptionPane.INFORMATION_MESSAGE);
    }
}
