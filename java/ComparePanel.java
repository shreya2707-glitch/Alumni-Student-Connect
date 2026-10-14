import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/** Side-by-side profile comparison. */
public class ComparePanel extends JPanel {
    private final MainFrame frame;
    private final JComboBox<Alumni> boxA = new JComboBox<>(), boxB = new JComboBox<>();
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"Attribute", "Alumni A", "Alumni B"}, 0) {
        public boolean isCellEditable(int r, int c) { return false; }
    };
    private boolean nextIsA = true;

    public ComparePanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        for (Alumni a : frame.all) { boxA.addItem(a); boxB.addItem(a); }
        if (frame.all.size() > 1) boxB.setSelectedIndex(1);

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Alumni A:")); top.add(boxA);
        top.add(new JLabel("  Alumni B:")); top.add(boxB);
        add(top, BorderLayout.NORTH);

        JTable table = new JTable(model);
        table.setRowHeight(42);
        table.getColumnModel().getColumn(0).setMaxWidth(160);
        table.setDefaultRenderer(Object.class, (t, v, sel, foc, r, c) -> {
            JLabel l = new JLabel("<html>" + v + "</html>");
            l.setOpaque(true);
            l.setBackground(c == 0 ? new Color(230, 233, 245) : Color.WHITE);
            l.setBorder(BorderFactory.createEmptyBorder(3, 6, 3, 6));
            return l;
        });
        add(new JScrollPane(table), BorderLayout.CENTER);

        boxA.addActionListener(e -> update());
        boxB.addActionListener(e -> update());
        update();
    }

    /** Called from an alumni card's Compare button. Alternates between slot A and B. */
    public void pick(Alumni a) {
        (nextIsA ? boxA : boxB).setSelectedItem(a);
        nextIsA = !nextIsA;
        frame.goTo(frame.compare);
    }

    public void update() {
        Alumni a = (Alumni) boxA.getSelectedItem(), b = (Alumni) boxB.getSelectedItem();
        if (a == null || b == null) return;
        model.setRowCount(0);
        row("Current role", a.getJobRole(), b.getJobRole());
        row("Company", a.getCompany(), b.getCompany());
        row("Industry", a.getIndustry(), b.getIndustry());
        row("Graduation year", "" + a.getGradYear(), "" + b.getGradYear());
        row("Qualification", a.getQualification(), b.getQualification());
        row("Skills", String.join(", ", a.getSkills()), String.join(", ", b.getSkills()));
        row("Interests", String.join(", ", a.getInterests()), String.join(", ", b.getInterests()));
        row("Career goal", a.getGoal(), b.getGoal());
        row("Career journey", a.getJourney(), b.getJourney());
        row("Shared skills", MatchEngine.shared(a.getSkills(), b.getSkills()).toString(), "(same)");
        if (frame.hasMatchData())
            row("Match with you", frame.scoreOf(a) + "%", frame.scoreOf(b) + "%");
    }

    private void row(String k, String a, String b) { model.addRow(new Object[]{k, a, b}); }
}
