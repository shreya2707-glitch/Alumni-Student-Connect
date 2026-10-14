import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Member4Launcher - Standalone Interactive Runner for Member 4.
 * Enables zero-dependency independent execution, testing, and evaluation
 * without touching Member 1's LoginFrame or existing files.
 */
public class Member4Launcher {

    public static void main(String[] args) {
        // Set modern look and feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(Member4Launcher::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        // 1. Role Selection Dialog to simulate session login
        String[] options = {
            "Student (ID: 1, Role: STUDENT)",
            "Alumni (ID: 2, Role: ALUMNI)"
        };

        JComboBox<String> roleSelector = new JComboBox<>(options);
        JPanel promptPanel = new JPanel(new GridLayout(0, 1, 6, 6));
        promptPanel.add(new JLabel("Select your test profile to launch Member 4:"));
        promptPanel.add(roleSelector);

        int choice = JOptionPane.showConfirmDialog(null, promptPanel,
                "Member 4 Standalone Launcher", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (choice != JOptionPane.OK_OPTION) {
            System.exit(0);
        }

        int selectedIndex = roleSelector.getSelectedIndex();
        int userId = (selectedIndex == 0) ? 1 : 2;
        String role = (selectedIndex == 0) ? "STUDENT" : "ALUMNI";

        // 2. Initialize Repository and Connection Pool
        CampusConnectRepository repo = new CampusConnectRepository();

        // 3. Create Main Frame
        JFrame frame = new JFrame("Alumni-Student Connect Platform | Member 4: Campus Connect & Live Activities");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1050, 700);
        frame.setLocationRelativeTo(null);

        // 4. Instantiate CampusConnectPanel
        CampusConnectPanel campusPanel = new CampusConnectPanel(userId, role, repo);
        frame.setContentPane(campusPanel);

        // 5. WindowListener to gracefully shut down background thread pools and connection pool on exit
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.out.println("[Member 4] Shutting down background worker threads and connection pool...");
                campusPanel.cleanup();
                repo.shutdown();
            }
        });

        frame.setVisible(true);
    }
}
