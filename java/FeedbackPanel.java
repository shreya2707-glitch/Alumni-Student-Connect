import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;

/** Member 5: view user feedback and update its status. */
public class FeedbackPanel extends JPanel {
    private final FeedbackRepository repo = new FeedbackRepository();
    private final DefaultTableModel model = Ui.model("ID", "User", "Rating", "Message", "Status", "Date");
    private final JTable table = Ui.table(model);
    private final List<Feedback> shown = new ArrayList<>();

    public FeedbackPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JButton reviewed = new JButton("Mark Reviewed"), actioned = new JButton("Mark Actioned"),
                create = new JButton("Add Feedback"), refresh = new JButton("Refresh");
        reviewed.addActionListener(e -> mark(Feedback.REVIEWED));
        actioned.addActionListener(e -> mark(Feedback.ACTIONED));
        create.addActionListener(e -> addFeedback());
        refresh.addActionListener(e -> load());
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(Ui.buttons(reviewed, actioned, create, refresh), BorderLayout.SOUTH);
        load();
    }

    public void load() {
        Ui.async(repo::all, list -> {
            model.setRowCount(0); shown.clear();
            for (Feedback x : list) {
                shown.add(x);
                model.addRow(new Object[]{x.getId(), x.getUserName(), x.getRating(), x.getMessage(), x.getStatus(), x.getCreatedAt()});
            }
        }, this);
    }

    private void mark(String status) {
        int row = table.getSelectedRow();
        if (row < 0 || row >= shown.size()) { Ui.error(this, "Select a feedback entry first."); return; }
        int id = shown.get(row).getId();
        Ui.async(() -> { repo.updateStatus(id, status); return true; }, ok -> load(), this);
    }

    private void addFeedback() {
        JTextField uid = new JTextField(6), rating = new JTextField(3), msg = new JTextField(25);
        JPanel f = new JPanel(new GridLayout(0, 1, 4, 4));
        f.add(new JLabel("User id:")); f.add(uid); f.add(new JLabel("Rating (1-5):")); f.add(rating);
        f.add(new JLabel("Message:")); f.add(msg);
        if (JOptionPane.showConfirmDialog(this, f, "Add Feedback", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        try {
            repo.create(Validator.positiveInt(uid.getText(), "User id"), msg.getText(), Validator.rating(rating.getText()));
            load();
        } catch (IllegalArgumentException ex) { Ui.error(this, ex.getMessage()); }
        catch (Exception ex) { Ui.error(this, "Database error: " + ex.getMessage()); }
    }
}
