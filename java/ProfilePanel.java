import javax.swing.*;
import java.awt.*;

/** Profile foundation: every role can view and edit their own profile. */
public class ProfilePanel extends JPanel {
    private final MainFrame frame;
    private final User user;
    private final JPanel form = new JPanel(new GridBagLayout());
    private int row = 0;

    private final JTextField name = new JTextField(28), email = new JTextField(28),
                             skills = new JTextField(28), interests = new JTextField(28);
    private final JTextArea bio = new JTextArea(3, 28);
    private JTextField company, jobRole, gradYear, qualification, journey;
    private JComboBox<String> industry, goal;

    public ProfilePanel(MainFrame frame, User user) {
        this.frame = frame; this.user = user;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        JLabel title = new JLabel(user.getWelcomeMessage() + "   [" + user.getType() + "]");
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        add(title, BorderLayout.NORTH);

        name.setText(user.getFullName()); email.setText(user.getEmail());
        bio.setText(user.getBio()); bio.setLineWrap(true);
        skills.setText(user.getSkillsCsv()); interests.setText(user.getInterestsCsv());

        addRow("Username", new JLabel(user.getUsername()));
        addRow("Full name", name);
        addRow("Email", email);
        addRow("Bio", new JScrollPane(bio));

        if (user instanceof Alumni) {
            Alumni a = (Alumni) user;
            company = new JTextField(a.getCompany(), 28);
            jobRole = new JTextField(a.getJobRole(), 28);
            gradYear = new JTextField("" + a.getGradYear(), 28);
            qualification = new JTextField(a.getQualification(), 28);
            journey = new JTextField(a.getJourney(), 28);
            industry = new JComboBox<>(QuizPanel.INDUSTRIES); industry.setSelectedItem(a.getIndustry());
            goal = new JComboBox<>(QuizPanel.GOALS); goal.setSelectedItem(a.getGoal());
            addRow("Company", company); addRow("Job role", jobRole); addRow("Industry", industry);
            addRow("Graduation year", gradYear); addRow("Qualification", qualification);
            addRow("Career goal", goal); addRow("Career journey", journey);
        } else if (user instanceof Student) {
            Student s = (Student) user;
            industry = new JComboBox<>(QuizPanel.INDUSTRIES); industry.setSelectedItem(s.getTargetIndustry());
            goal = new JComboBox<>(QuizPanel.GOALS); goal.setSelectedItem(s.getGoal());
            addRow("Target industry", industry); addRow("Career goal", goal);
        }
        if (!(user instanceof Admin)) { addRow("Skills (comma separated)", skills); addRow("Interests (comma separated)", interests); }

        add(new JScrollPane(form), BorderLayout.CENTER);
        JButton save = new JButton("Save profile");
        save.addActionListener(e -> save());
        add(save, BorderLayout.SOUTH);
    }

    private void addRow(String label, JComponent c) {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 6, 4, 6); g.gridy = row++;
        g.gridx = 0; g.anchor = GridBagConstraints.EAST; form.add(new JLabel(label + ":"), g);
        g.gridx = 1; g.anchor = GridBagConstraints.WEST; form.add(c, g);
    }

    private void save() {
        try {
            user.setFullName(name.getText().trim());
            user.setEmail(email.getText().trim());
            user.setBio(bio.getText().trim());
            if (!(user instanceof Admin)) {
                user.setSkillsCsv(skills.getText());
                user.setInterestsCsv(interests.getText());
            }
            if (user instanceof Alumni) {
                Alumni a = (Alumni) user;
                a.setCompany(company.getText().trim()); a.setJobRole(jobRole.getText().trim());
                a.setIndustry((String) industry.getSelectedItem());
                a.setGradYear(Integer.parseInt(gradYear.getText().trim()));
                a.setQualification(qualification.getText().trim()); a.setJourney(journey.getText().trim());
                a.setGoal((String) goal.getSelectedItem());
            } else if (user instanceof Student) {
                Student s = (Student) user;
                s.setTargetIndustry((String) industry.getSelectedItem());
                s.setGoal((String) goal.getSelectedItem());
            }
            AppContext.users.updateProfile(user);
            frame.onProfileChanged();
            JOptionPane.showMessageDialog(this, "Profile saved.");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Graduation year must be a number.");
        }
    }
}
