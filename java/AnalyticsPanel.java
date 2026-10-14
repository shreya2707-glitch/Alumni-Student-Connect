import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Map;

/** Member 5: platform statistics (aggregation queries). */
public class AnalyticsPanel extends JPanel {
    private final ImpactRepository repo = new ImpactRepository();
    private final DefaultTableModel summary = Ui.model("Metric", "Value");
    private final DefaultTableModel monthly = Ui.model("Month", "Activity", "Count");

    public AnalyticsPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(e -> load());
        JPanel left = new JPanel(new BorderLayout(4, 4));
        left.add(new JLabel("Platform summary"), BorderLayout.NORTH);
        left.add(new JScrollPane(Ui.table(summary)), BorderLayout.CENTER);
        JPanel right = new JPanel(new BorderLayout(4, 4));
        right.add(new JLabel("Monthly activity"), BorderLayout.NORTH);
        right.add(new JScrollPane(Ui.table(monthly)), BorderLayout.CENTER);
        add(new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right), BorderLayout.CENTER);
        add(Ui.buttons(refresh), BorderLayout.SOUTH);
        load();
    }

    @SuppressWarnings("unchecked")
    public void load() {
        Ui.async(() -> new Object[]{repo.summary(), repo.monthlyActivity()}, d -> {
            summary.setRowCount(0); monthly.setRowCount(0);
            for (Map.Entry<String, String> e : ((Map<String, String>) d[0]).entrySet()) summary.addRow(new Object[]{e.getKey(), e.getValue()});
            for (Object[] r : (java.util.List<Object[]>) d[1]) monthly.addRow(r);
        }, this);
    }
}
