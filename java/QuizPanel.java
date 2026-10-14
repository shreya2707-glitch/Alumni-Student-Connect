import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/** Career DNA quiz. Saves the answers into the logged-in Student's profile. */
public class QuizPanel extends JPanel {
    static final String[] SKILLS = {"Java","Python","SQL","Data Analysis","Machine Learning","UI/UX",
        "Cloud","Communication","Leadership","Finance","Marketing","Cybersecurity"};
    static final String[] INTERESTS = {"AI","Startups","Research","Design","Finance","Healthcare",
        "Gaming","Consulting","Teaching","Security"};
    static final String[] INDUSTRIES = {"Technology","Finance","Healthcare","Consulting","Education","Design"};
    static final String[] GOALS = {"Technical Expert","Team Leader","Entrepreneur","Researcher"};

    private final MainFrame frame;
    private final Student student;
    private final List<JCheckBox> skillBoxes = new ArrayList<>(), interestBoxes = new ArrayList<>();
    private final JComboBox<String> industryBox = new JComboBox<>(INDUSTRIES);
    private final JComboBox<String> goalBox = new JComboBox<>(GOALS);

    public QuizPanel(MainFrame frame, Student student) {
        this.frame = frame; this.student = student;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        JLabel title = new JLabel("Discover your Career DNA");
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        add(title, BorderLayout.NORTH);

        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.add(checkGroup("1. Which skills do you have?", SKILLS, skillBoxes, student.getSkills()));
        form.add(checkGroup("2. What kind of work interests you?", INTERESTS, interestBoxes, student.getInterests()));
        JPanel p3 = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p3.add(new JLabel("3. Target industry:")); p3.add(industryBox);
        p3.add(new JLabel("   4. Career goal:")); p3.add(goalBox);
        industryBox.setSelectedItem(student.getTargetIndustry());
        goalBox.setSelectedItem(student.getGoal());
        form.add(p3);
        add(new JScrollPane(form), BorderLayout.CENTER);

        JButton go = new JButton("Reveal my matches");
        go.setFont(new Font("SansSerif", Font.BOLD, 15));
        go.addActionListener(e -> submit());
        add(go, BorderLayout.SOUTH);
    }

    private JPanel checkGroup(String title, String[] items, List<JCheckBox> store, List<String> preset) {
        JPanel p = new JPanel(new GridLayout(0, 4));
        p.setBorder(BorderFactory.createTitledBorder(title));
        for (String s : items) {
            JCheckBox c = new JCheckBox(s);
            for (String x : preset) if (x.equalsIgnoreCase(s)) c.setSelected(true);
            store.add(c); p.add(c);
        }
        return p;
    }

    private String selected(List<JCheckBox> boxes) {
        StringBuilder sb = new StringBuilder();
        for (JCheckBox c : boxes) if (c.isSelected()) sb.append(sb.length() > 0 ? "," : "").append(c.getText());
        return sb.toString();
    }

    private void submit() {
        String skills = selected(skillBoxes), interests = selected(interestBoxes);
        if (skills.isEmpty() || interests.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please pick at least one skill and one interest.");
            return;
        }
        student.setSkillsCsv(skills);
        student.setInterestsCsv(interests);
        student.setTargetIndustry((String) industryBox.getSelectedItem());
        student.setGoal((String) goalBox.getSelectedItem());
        AppContext.users.updateProfile(student);          // persisted via the repository
        frame.onProfileChanged();
        JOptionPane.showMessageDialog(this,
            "Your Career DNA points toward: " + MatchEngine.careerDNA(student, frame.all, AppContext.matcher) +
            "\nOpening your matched alumni...", "Career DNA", JOptionPane.INFORMATION_MESSAGE);
        frame.goTo(frame.dash);
    }
}
