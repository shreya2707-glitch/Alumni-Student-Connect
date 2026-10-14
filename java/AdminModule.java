import javax.swing.*;

/** Member 5: plugs the Admin Console into the shared app (same pattern as Member 3's JobBoardModule). */
public class AdminModule implements FeatureModule {
    @Override public String getTitle() { return "Admin Console"; }
    @Override public boolean isVisibleFor(Role role) { return role == Role.ADMIN; }
    @Override public JComponent createPanel(User currentUser) {
        if (!DBConnection.isAvailable()) {
            JPanel p = new JPanel();
            p.add(new JLabel("Admin Console needs MySQL. Start MySQL and run sql/M5_SCHEMA.sql."));
            return p;
        }
        return new AdminDashboardPanel(currentUser);
    }
}
