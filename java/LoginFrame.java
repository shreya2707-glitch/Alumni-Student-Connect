import javax.swing.*;
import java.awt.*;

/** Login + registration screen. */
public class LoginFrame extends JFrame {
    private final JTextField loginUser = new JTextField(18);
    private final JPasswordField loginPass = new JPasswordField(18);

    private final JTextField regUser = new JTextField(18), regName = new JTextField(18), regEmail = new JTextField(18);
    private final JPasswordField regPass = new JPasswordField(18);
    private final JComboBox<String> regRole = new JComboBox<>(new String[]{"Student", "Alumni"});
    private final JTextField regCompany = new JTextField(18), regJob = new JTextField(18), regYear = new JTextField(18);
    private final JComboBox<String> regIndustry = new JComboBox<>(QuizPanel.INDUSTRIES);
    private final JLabel alumniLbl1 = new JLabel("Company:"), alumniLbl2 = new JLabel("Job role:"),
                         alumniLbl3 = new JLabel("Industry:"), alumniLbl4 = new JLabel("Graduation year:");

    public LoginFrame() {
        super("Smart Alumni Network - Sign in");
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Login", loginPanel());
        tabs.addTab("Register", registerPanel());
        add(tabs);
        add(new JLabel("<html><center>Demo: student1 / student123 &nbsp;|&nbsp; aarav / alumni123 &nbsp;|&nbsp; admin / admin123</center></html>",
                SwingConstants.CENTER), BorderLayout.SOUTH);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(460, 440);
        setLocationRelativeTo(null);
    }

    private int row;
    private void addRow(JPanel p, JComponent label, JComponent field) {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 8, 5, 8); g.gridy = row++;
        g.gridx = 0; g.anchor = GridBagConstraints.EAST; p.add(label, g);
        g.gridx = 1; g.anchor = GridBagConstraints.WEST; p.add(field, g);
    }

    private JPanel loginPanel() {
        JPanel p = new JPanel(new GridBagLayout()); row = 0;
        addRow(p, new JLabel("Username:"), loginUser);
        addRow(p, new JLabel("Password:"), loginPass);
        JButton b = new JButton("Login");
        loginPass.addActionListener(e -> doLogin());
        b.addActionListener(e -> doLogin());
        addRow(p, new JLabel(), b);
        return p;
    }

    private JPanel registerPanel() {
        JPanel p = new JPanel(new GridBagLayout()); row = 0;
        addRow(p, new JLabel("I am a:"), regRole);
        addRow(p, new JLabel("Full name:"), regName);
        addRow(p, new JLabel("Email:"), regEmail);
        addRow(p, new JLabel("Username:"), regUser);
        addRow(p, new JLabel("Password (6+):"), regPass);
        addRow(p, alumniLbl1, regCompany);
        addRow(p, alumniLbl2, regJob);
        addRow(p, alumniLbl3, regIndustry);
        addRow(p, alumniLbl4, regYear);
        JButton b = new JButton("Create account");
        b.addActionListener(e -> doRegister());
        addRow(p, new JLabel(), b);
        regRole.addActionListener(e -> toggleAlumniFields());
        toggleAlumniFields();
        return p;
    }

    private void toggleAlumniFields() {
        boolean alumni = regRole.getSelectedIndex() == 1;
        for (JComponent c : new JComponent[]{alumniLbl1, alumniLbl2, alumniLbl3, alumniLbl4,
                                             regCompany, regJob, regIndustry, regYear}) c.setVisible(alumni);
    }

    private void doLogin() {
        try {
            User u = AppContext.auth.login(loginUser.getText(), new String(loginPass.getPassword()));
            open(u);
        } catch (AuthException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Login failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void doRegister() {
        try {
            String user = regUser.getText().trim(), name = regName.getText().trim(), email = regEmail.getText().trim();
            String pass = new String(regPass.getPassword());
            User u;
            if (regRole.getSelectedIndex() == 0) {
                u = new Student(0, user, name, email, "", "", "Technology", "Technical Expert");
            } else {
                int year;
                try { year = Integer.parseInt(regYear.getText().trim()); }
                catch (NumberFormatException e) { throw new AuthException("Graduation year must be a number."); }
                u = new Alumni(0, user, name, email, regCompany.getText().trim(), regJob.getText().trim(),
                        (String) regIndustry.getSelectedItem(), year, "", "", "Technical Expert", "", "");
            }
            AppContext.auth.register(u, pass);
            open(AppContext.auth.login(user, pass));
        } catch (AuthException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Registration failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void open(User u) {
        new MainFrame(u).setVisible(true);
        dispose();
    }
}
