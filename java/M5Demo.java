import javax.swing.*;
import java.sql.*;

/** Member 5: run the Admin Console alone (before integration). java -cp "out:lib/*" M5Demo */
public class M5Demo {
    public static void main(String[] args) throws Exception {
        if (!DBConnection.isAvailable()) { System.out.println("MySQL not reachable. Check DBConnection + run M5_SCHEMA.sql"); return; }
        int adminId = 0; String name = "Demo Admin";
        try (Connection c = DBConnection.get(); Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT id, full_name FROM users WHERE user_role='ADMIN' LIMIT 1")) {
            if (rs.next()) { adminId = rs.getInt(1); name = rs.getString(2); }
        }
        User admin = new User(adminId, "admin", name, "admin@alumni.edu", Role.ADMIN, "", "") {
            @Override public String getSummary() { return "Administrator"; }
        };
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Smart Alumni Network - Admin Console (Member 5)");
            f.add(new AdminModule().createPanel(admin));
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setSize(1000, 650); f.setLocationRelativeTo(null); f.setVisible(true);
        });
    }
}
