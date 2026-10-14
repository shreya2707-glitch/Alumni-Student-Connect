import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/** Member 5: badge thresholds and earned achievements. */
public class BadgePanel extends JPanel {
    private final ImpactRepository repo = new ImpactRepository();
    private final DefaultTableModel badgeModel = Ui.model("Badge", "Minimum score");
    private final DefaultTableModel achModel = Ui.model("User", "Badge", "Awarded on");
    private final JTable badgeTable = Ui.table(badgeModel);
    private List<Badge> badges = new java.util.ArrayList<>();

    public BadgePanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JButton refresh = new JButton("Refresh"), edit = new JButton("Change Minimum Score");
        refresh.addActionListener(e -> load());
        edit.addActionListener(e -> editThreshold());
        JPanel top = new JPanel(new BorderLayout(4, 4));
        top.add(new JLabel("Badge levels"), BorderLayout.NORTH);
        JScrollPane sp = new JScrollPane(badgeTable); sp.setPreferredSize(new Dimension(300, 110));
        top.add(sp, BorderLayout.CENTER);
        top.add(Ui.buttons(edit, refresh), BorderLayout.SOUTH);
        JPanel bottom = new JPanel(new BorderLayout(4, 4));
        bottom.add(new JLabel("Achievements earned"), BorderLayout.NORTH);
        bottom.add(new JScrollPane(Ui.table(achModel)), BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);
        add(bottom, BorderLayout.CENTER);
        load();
    }

    @SuppressWarnings("unchecked")
    public void load() {
        Ui.async(() -> new Object[]{repo.badges(), repo.achievements()}, d -> {
            badges = (List<Badge>) d[0];
            badgeModel.setRowCount(0); achModel.setRowCount(0);
            for (Badge b : badges) badgeModel.addRow(new Object[]{b.getName(), b.getMinScore()});
            for (Achievement a : (List<Achievement>) d[1]) achModel.addRow(new Object[]{a.getUserName(), a.getBadge(), a.getAwardedAt()});
        }, this);
    }

    private void editThreshold() {
        int row = badgeTable.getSelectedRow();
        if (row < 0 || row >= badges.size()) { Ui.error(this, "Select a badge first."); return; }
        Badge b = badges.get(row);
        String s = JOptionPane.showInputDialog(this, "New minimum score for " + b.getName() + ":", b.getMinScore());
        if (s == null) return;
        int v;
        try { v = Integer.parseInt(s.trim()); } catch (NumberFormatException ex) { Ui.error(this, "Enter a whole number."); return; }
        if (v < 0) { Ui.error(this, "Minimum score cannot be negative."); return; }
        Ui.async(() -> { repo.updateMinScore(b.getId(), v); return true; }, ok -> load(), this);
    }
}
