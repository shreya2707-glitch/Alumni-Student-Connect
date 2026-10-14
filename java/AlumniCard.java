import javax.swing.*;
import java.awt.*;

/** One alumni card shown on the dashboard. */
public class AlumniCard extends JPanel {
    public AlumniCard(Alumni a, int score, boolean hasProfile, Runnable onCompare, Runnable onView) {
        setLayout(new BorderLayout(6, 6));
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(190, 195, 210)),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)));

        JLabel name = new JLabel(a.getName());
        name.setFont(new Font("SansSerif", Font.BOLD, 15));
        JLabel role = new JLabel(a.getJobRole() + " @ " + a.getCompany());
        JLabel meta = new JLabel(a.getIndustry() + "  |  Class of " + a.getGradYear());
        meta.setForeground(Color.GRAY);
        JPanel top = new JPanel(new GridLayout(3, 1));
        top.add(name); top.add(role); top.add(meta);
        add(top, BorderLayout.NORTH);

        JLabel sk = new JLabel("<html><b>Skills:</b> " + String.join(", ", a.getSkills()) + "</html>");
        add(sk, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(5, 5));
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setStringPainted(true);
        if (hasProfile) { bar.setValue(score); bar.setString("Match " + score + "%"); }
        else { bar.setValue(0); bar.setString("Take quiz for score"); }
        bottom.add(bar, BorderLayout.NORTH);
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        JButton view = new JButton("View"), cmp = new JButton("Compare");
        view.addActionListener(e -> onView.run());
        cmp.addActionListener(e -> onCompare.run());
        btns.add(view); btns.add(cmp);
        bottom.add(btns, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);
    }
}
