import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/** Member 5: small Swing helpers shared by all admin screens. */
public final class Ui {
    private Ui() {}
    public static DefaultTableModel model(String... cols) {
        return new DefaultTableModel(cols, 0) { @Override public boolean isCellEditable(int r, int c) { return false; } };
    }
    public static JTable table(DefaultTableModel m) {
        JTable t = new JTable(m);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setRowHeight(22);
        t.getTableHeader().setReorderingAllowed(false);
        return t;
    }
    public static JPanel buttons(JButton... bs) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (JButton b : bs) p.add(b);
        return p;
    }
    /** Runs DB work on a background thread (SwingWorker) so the window never freezes; onDone runs on the UI thread. */
    public static <T> void async(java.util.concurrent.Callable<T> work, java.util.function.Consumer<T> onDone, Component parent) {
        new SwingWorker<T, Void>() {
            @Override protected T doInBackground() throws Exception { return work.call(); }
            @Override protected void done() {
                try { onDone.accept(get()); }
                catch (Exception ex) { Throwable c = ex.getCause() == null ? ex : ex.getCause(); error(parent, "Database error: " + c.getMessage()); }
            }
        }.execute();
    }
    public static void error(Component p, String m) { JOptionPane.showMessageDialog(p, m, "Admin Console", JOptionPane.WARNING_MESSAGE); }
    public static void info(Component p, String m)  { JOptionPane.showMessageDialog(p, m, "Admin Console", JOptionPane.INFORMATION_MESSAGE); }
}
