import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;

/** Member 5: moderation of users AND Member 4 content (Q&A answers, discussion posts). */
public class ModerationPanel extends JPanel {
    private final ReportRepository repo = new ReportRepository();
    private final int adminId;
    private final DefaultTableModel model = Ui.model("ID", "Reporter", "Reported user", "Target", "Reason", "Status", "Date", "Admin note");
    private final JTable table = Ui.table(model);
    private final JComboBox<String> filter = new JComboBox<>(new String[]{"PENDING", "ALL", "RESOLVED", "DISMISSED"});
    private final List<Report> shown = new ArrayList<>();

    public ModerationPanel(User admin) {
        this.adminId = admin.getId();
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Show:")); top.add(filter);
        filter.addActionListener(e -> load());
        JButton resolve = new JButton("Resolve"), dismiss = new JButton("Dismiss"), block = new JButton("Block Reported User"),
                del = new JButton("Delete Reported Content"), create = new JButton("New Report"), refresh = new JButton("Refresh");
        resolve.addActionListener(e -> close(Report.RESOLVED));
        dismiss.addActionListener(e -> close(Report.DISMISSED));
        block.addActionListener(e -> { Report r = selectedPending(); if (r != null && confirm("Block " + r.getReportedName() + " and resolve this report?"))
            Ui.async(() -> { repo.blockAndResolve(r, adminId); return true; }, ok -> load(), this); });
        del.addActionListener(e -> { Report r = selectedPending();
            if (r == null) return;
            if (!r.isContent()) { Ui.error(this, "This report is about a user. Use Block, Resolve or Dismiss."); return; }
            if (confirm("Delete " + r.getTargetLabel() + " permanently and resolve this report?"))
                Ui.async(() -> { repo.deleteContentAndResolve(r, adminId); return true; }, ok -> load(), this); });
        create.addActionListener(e -> newReport());
        refresh.addActionListener(e -> load());
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(Ui.buttons(resolve, dismiss, block, del, create, refresh), BorderLayout.SOUTH);
        load();
    }

    public void load() {
        Ui.async(() -> repo.find((String) filter.getSelectedItem()), list -> {
            model.setRowCount(0); shown.clear();
            for (Report r : list) {
                shown.add(r);
                model.addRow(new Object[]{r.getId(), r.getReporterName(), r.getReportedName(), r.getTargetLabel(), r.getReason(),
                        r.getStatus(), r.getCreatedAt(), r.getNote()});
            }
        }, this);
    }

    private boolean confirm(String msg) { return JOptionPane.showConfirmDialog(this, msg, "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION; }

    private Report selectedPending() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= shown.size()) { Ui.error(this, "Select a report first."); return null; }
        Report r = shown.get(row);
        if (!r.isPending()) { Ui.error(this, "This report is already " + r.getStatus() + "."); return null; }
        return r;
    }

    private void close(String status) {
        Report r = selectedPending();
        if (r == null) return;
        String note = JOptionPane.showInputDialog(this, "Note for report #" + r.getId() + " (optional):");
        if (note == null) return;
        Ui.async(() -> repo.close(r.getId(), adminId, status, note.trim()), ok -> {
            if (!ok) Ui.error(this, "Report was already closed.");
            load();
        }, this);
    }

    private void newReport() {
        JComboBox<String> type = new JComboBox<>(new String[]{Report.USER, Report.QA_ANSWER, Report.DISCUSSION_POST});
        JTextField reporter = new JTextField(6), target = new JTextField(6), reason = new JTextField(25);
        JPanel f = new JPanel(new GridLayout(0, 1, 4, 4));
        f.add(new JLabel("What is reported:")); f.add(type);
        f.add(new JLabel("Reporter user id:")); f.add(reporter);
        f.add(new JLabel("Target id (user id / answer id / post id):")); f.add(target);
        f.add(new JLabel("Reason:")); f.add(reason);
        if (JOptionPane.showConfirmDialog(this, f, "New Report", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        final int rid, tid;
        try { rid = Validator.positiveInt(reporter.getText(), "Reporter id"); tid = Validator.positiveInt(target.getText(), "Target id"); }
        catch (IllegalArgumentException ex) { Ui.error(this, ex.getMessage()); return; }
        String t = (String) type.getSelectedItem();
        Ui.async(() -> {
            int author = Report.USER.equals(t) ? tid : repo.authorOf(t, tid);
            return repo.create(rid, author, t, tid, reason.getText());
        }, id -> load(), this);
    }
}
