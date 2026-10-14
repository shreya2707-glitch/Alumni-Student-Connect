import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * Member 3: Opportunity Arena screen.
 * Student tabs : Opportunities, Swipe Cards, Saved, My Applications, Skill Gap, Resume Checklist, Referrals.
 * Alumni tabs  : Referral Inbox, Post a Job.
 */
public class JobBoardPanel extends JPanel {
    private static final String TITLE = "Opportunity Arena";
    private final User currentUser;
    private final JobRepository jobRepo = new JobRepository();
    private final ApplicationRepository appRepo = new ApplicationRepository();
    private final ReferralRepository referralRepo = new ReferralRepository();
    private final JTabbedPane innerTabs = new JTabbedPane();
    private final Map<Component, Runnable> tabRefresh = new HashMap<>();   // refresh a tab when it is opened

    public JobBoardPanel(User currentUser) {
        this.currentUser = currentUser;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JLabel heading = new JLabel("Opportunity Arena  |  Jobs, internships and hiring.");
        heading.setFont(new Font("SansSerif", Font.BOLD, 17));
        add(heading, BorderLayout.NORTH);
        if (currentUser instanceof Student) {
            innerTabs.addTab("Opportunities", buildOpportunitiesTab());
            innerTabs.addTab("Swipe Cards", buildSwipeTab());
            innerTabs.addTab("Saved", buildSavedTab());
            innerTabs.addTab("My Applications", buildApplicationsTab());
            innerTabs.addTab("Skill Gap", buildSkillGapTab());
            innerTabs.addTab("Resume Checklist", buildResumeTab());
            innerTabs.addTab("Referrals", buildStudentReferralTab());
        } else if (currentUser instanceof Alumni) {
            innerTabs.addTab("Referral Inbox", buildAlumniInboxTab());
            innerTabs.addTab("Post a Job", buildPostJobTab());
        }
        innerTabs.addChangeListener(e -> {
            Runnable r = tabRefresh.get(innerTabs.getSelectedComponent());
            if (r != null) r.run();
        });
        add(innerTabs, BorderLayout.CENTER);
    }

    private Student student() { return (Student) currentUser; }

    // =====================================================================
    //  STUDENT: Opportunities (list, search, filter, details, save, skip, apply)
    // =====================================================================
    private JPanel buildOpportunitiesTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JTextField search = new JTextField(20);
        JComboBox<String> type = new JComboBox<>(new String[]{"ALL", "JOB", "INTERNSHIP"});
        JButton searchBtn = new JButton("Search"), reset = new JButton("Reset");
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Search (title, company, skill, city):")); top.add(search);
        top.add(new JLabel("Type:")); top.add(type); top.add(searchBtn); top.add(reset);

        DefaultTableModel model = newModel("Title", "Company", "Type", "Location", "Match %", "Your Action");
        JTable table = newTable(model);
        List<Job> shown = new ArrayList<>();
        Runnable reload = () -> {
            model.setRowCount(0); shown.clear();
            try {
                Map<Integer, String> actions = jobRepo.actionsFor(currentUser.getId());
                for (Job j : jobRepo.search(search.getText(), (String) type.getSelectedItem())) {
                    shown.add(j);
                    String act = actions.get(j.getId());
                    model.addRow(new Object[]{j.getTitle(), j.getCompany(), j.getJobType(), j.getLocation(), matchPercent(j) + "%", act == null ? "-" : act});
                }
            } catch (Exception ex) { showError("Could not load opportunities: " + ex.getMessage()); }
        };
        search.addActionListener(e -> reload.run());
        searchBtn.addActionListener(e -> reload.run());
        reset.addActionListener(e -> { search.setText(""); type.setSelectedIndex(0); reload.run(); });
        type.addActionListener(e -> reload.run());

        JButton details = new JButton("View Details"), why = new JButton("Why am I a match?"),
                save = new JButton("Save"), skip = new JButton("Skip"), apply = new JButton("Apply"), refresh = new JButton("Refresh");
        details.addActionListener(e -> { Job j = selected(table, shown, "an opportunity"); if (j != null) showDetails(j); });
        why.addActionListener(e -> { Job j = selected(table, shown, "an opportunity"); if (j != null) showMatch(j); });
        save.addActionListener(e -> { Job j = selected(table, shown, "an opportunity"); if (j != null) markJob(j, "SAVED", reload); });
        skip.addActionListener(e -> { Job j = selected(table, shown, "an opportunity"); if (j != null) markJob(j, "SKIPPED", reload); });
        apply.addActionListener(e -> { Job j = selected(table, shown, "an opportunity"); if (j != null) applyTo(j); });
        refresh.addActionListener(e -> reload.run());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(details); buttons.add(why); buttons.add(save); buttons.add(skip); buttons.add(apply); buttons.add(refresh);

        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        tabRefresh.put(panel, reload);
        reload.run();
        return panel;
    }

    // =====================================================================
    //  STUDENT: Swipe-style cards (Save / Skip / View Details)
    // =====================================================================
    private JPanel buildSwipeTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JLabel counter = new JLabel(" ", SwingConstants.CENTER);
        JLabel title = new JLabel(" ", SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, 22));
        JLabel sub = new JLabel(" ", SwingConstants.CENTER);
        JLabel score = new JLabel(" ", SwingConstants.CENTER);
        score.setFont(new Font("SansSerif", Font.BOLD, 16));
        JTextArea body = new JTextArea();
        body.setEditable(false); body.setLineWrap(true); body.setWrapStyleWord(true);
        body.setFont(new Font("SansSerif", Font.PLAIN, 14));
        JPanel head = new JPanel(new GridLayout(0, 1, 2, 2));
        head.add(title); head.add(sub); head.add(score);
        JPanel card = new JPanel(new BorderLayout(6, 6));
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.GRAY, 2, true),
            BorderFactory.createEmptyBorder(16, 16, 16, 16)));
        card.add(head, BorderLayout.NORTH);
        card.add(new JScrollPane(body), BorderLayout.CENTER);
        JPanel cardWrap = new JPanel(new BorderLayout());
        cardWrap.setBorder(BorderFactory.createEmptyBorder(0, 100, 0, 100));
        cardWrap.add(card, BorderLayout.CENTER);

        List<Job> deck = new ArrayList<>();
        Runnable showCard = () -> {
            if (deck.isEmpty()) {
                title.setText("No more cards"); sub.setText(" "); score.setText(" ");
                body.setText("You have swiped through every opportunity.\nOpen the Saved tab to see what you kept, or press \"Bring Back Skipped\".");
                counter.setText("0 cards left");
            } else {
                Job j = deck.get(0);
                title.setText(j.getTitle());
                sub.setText(j.getCompany() + "   |   " + j.getJobType() + "   |   " + j.getLocation());
                score.setText("Match: " + matchPercent(j) + "%");
                body.setText("Required skills: " + j.getRequiredSkillsCsv() + "\n\n" + j.getDescription());
                body.setCaretPosition(0);
                counter.setText(deck.size() + " card(s) left");
            }
        };
        Runnable reloadDeck = () -> {
            deck.clear();
            try { deck.addAll(jobRepo.unseenJobs(currentUser.getId())); }
            catch (Exception ex) { showError("Could not load cards: " + ex.getMessage()); }
            showCard.run();
        };
        Consumer<String> swipe = action -> {
            if (deck.isEmpty()) { showError("There are no more cards."); return; }
            try { jobRepo.setAction(currentUser.getId(), deck.get(0).getId(), action); deck.remove(0); showCard.run(); }
            catch (Exception ex) { showError("Could not update the card: " + ex.getMessage()); }
        };
        JButton skip = new JButton("\u2716 Skip"), details = new JButton("View Details"), why = new JButton("Why am I a match?"),
                save = new JButton("\u2714 Save"), apply = new JButton("Apply"), back = new JButton("Bring Back Skipped");
        skip.addActionListener(e -> swipe.accept("SKIPPED"));
        save.addActionListener(e -> swipe.accept("SAVED"));
        details.addActionListener(e -> { if (deck.isEmpty()) showError("There are no more cards."); else showDetails(deck.get(0)); });
        why.addActionListener(e -> { if (deck.isEmpty()) showError("There are no more cards."); else showMatch(deck.get(0)); });
        apply.addActionListener(e -> { if (deck.isEmpty()) showError("There are no more cards."); else applyTo(deck.get(0)); });
        back.addActionListener(e -> {
            try { jobRepo.clearSkipped(currentUser.getId()); reloadDeck.run(); }
            catch (Exception ex) { showError("Could not restore skipped jobs: " + ex.getMessage()); }
        });
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttons.add(skip); buttons.add(details); buttons.add(why); buttons.add(save); buttons.add(apply); buttons.add(back);

        panel.add(counter, BorderLayout.NORTH);
        panel.add(cardWrap, BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        tabRefresh.put(panel, reloadDeck);
        reloadDeck.run();
        return panel;
    }

    // =====================================================================
    //  STUDENT: Saved opportunities
    // =====================================================================
    private JPanel buildSavedTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        DefaultTableModel model = newModel("Title", "Company", "Type", "Location", "Match %");
        JTable table = newTable(model);
        List<Job> shown = new ArrayList<>();
        Runnable reload = () -> {
            model.setRowCount(0); shown.clear();
            try {
                for (Job j : jobRepo.jobsWithAction(currentUser.getId(), "SAVED")) {
                    shown.add(j);
                    model.addRow(new Object[]{j.getTitle(), j.getCompany(), j.getJobType(), j.getLocation(), matchPercent(j) + "%"});
                }
            } catch (Exception ex) { showError("Could not load saved opportunities: " + ex.getMessage()); }
        };
        JButton details = new JButton("View Details"), why = new JButton("Why am I a match?"), apply = new JButton("Apply"),
                remove = new JButton("Remove from Saved"), refresh = new JButton("Refresh");
        details.addActionListener(e -> { Job j = selected(table, shown, "a saved opportunity"); if (j != null) showDetails(j); });
        why.addActionListener(e -> { Job j = selected(table, shown, "a saved opportunity"); if (j != null) showMatch(j); });
        apply.addActionListener(e -> { Job j = selected(table, shown, "a saved opportunity"); if (j != null) applyTo(j); });
        remove.addActionListener(e -> {
            Job j = selected(table, shown, "a saved opportunity");
            if (j == null) return;
            try { jobRepo.clearAction(currentUser.getId(), j.getId()); reload.run(); }
            catch (Exception ex) { showError("Could not remove it: " + ex.getMessage()); }
        });
        refresh.addActionListener(e -> reload.run());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(details); buttons.add(why); buttons.add(apply); buttons.add(remove); buttons.add(refresh);
        panel.add(new JLabel("Opportunities you saved:"), BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        tabRefresh.put(panel, reload);
        reload.run();
        return panel;
    }

    // =====================================================================
    //  STUDENT: Application Tracker (create / read / update / delete)
    // =====================================================================
    private JPanel buildApplicationsTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JLabel summary = new JLabel(" ");
        DefaultTableModel model = newModel("App ID", "Job", "Company", "Status", "Applied On", "Notes");
        JTable table = newTable(model);
        List<Application> shown = new ArrayList<>();
        Runnable reload = () -> {
            model.setRowCount(0); shown.clear();
            try {
                Map<String, Integer> counts = new LinkedHashMap<>();
                for (String s : Application.STATUSES) counts.put(s, 0);
                for (Application a : appRepo.forStudent(currentUser.getId())) {
                    shown.add(a);
                    counts.put(a.getStatus(), counts.get(a.getStatus()) + 1);
                    model.addRow(new Object[]{a.getId(), a.getJobTitle(), a.getCompany(), a.getStatus(), a.getAppliedAt(), a.getNotes()});
                }
                StringBuilder sb = new StringBuilder("Total: " + shown.size());
                for (Map.Entry<String, Integer> en : counts.entrySet()) sb.append("   |   ").append(en.getKey()).append(": ").append(en.getValue());
                summary.setText(sb.toString());
            } catch (Exception ex) { showError("Could not load applications: " + ex.getMessage()); }
        };
        JButton details = new JButton("View Details"), update = new JButton("Update Status"),
                withdraw = new JButton("Withdraw Application"), refresh = new JButton("Refresh");
        details.addActionListener(e -> { Application a = selected(table, shown, "an application"); if (a != null) showApplication(a); });
        update.addActionListener(e -> {
            Application a = selected(table, shown, "an application");
            if (a == null) return;
            JComboBox<String> status = new JComboBox<>(Application.STATUSES);
            status.setSelectedItem(a.getStatus());
            JTextField notes = new JTextField(a.getNotes(), 25);
            JPanel fields = new JPanel(new GridLayout(0, 1, 4, 4));
            fields.add(new JLabel("Status:")); fields.add(status); fields.add(new JLabel("Notes (optional):")); fields.add(notes);
            if (JOptionPane.showConfirmDialog(this, fields, "Update Application", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                try { appRepo.updateStatus(a.getId(), currentUser.getId(), (String) status.getSelectedItem(), notes.getText()); reload.run(); }
                catch (Exception ex) { showError("Could not update the application: " + ex.getMessage()); }
            }
        });
        withdraw.addActionListener(e -> {
            Application a = selected(table, shown, "an application");
            if (a == null) return;
            if (JOptionPane.showConfirmDialog(this, "Withdraw your application to " + a.getJobTitle() + " at " + a.getCompany() + "?",
                    TITLE, JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
            try { appRepo.delete(a.getId(), currentUser.getId()); reload.run(); }
            catch (Exception ex) { showError("Could not withdraw the application: " + ex.getMessage()); }
        });
        refresh.addActionListener(e -> reload.run());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(details); buttons.add(update); buttons.add(withdraw); buttons.add(refresh);
        panel.add(summary, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        tabRefresh.put(panel, reload);
        reload.run();
        return panel;
    }

    // =====================================================================
    //  STUDENT: Skill Gap Checker
    // =====================================================================
    private JPanel buildSkillGapTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JComboBox<Job> jobBox = new JComboBox<>();
        JProgressBar bar = new JProgressBar(0, 100);
        bar.setStringPainted(true);
        JTextArea result = new JTextArea();
        result.setEditable(false); result.setFont(new Font("SansSerif", Font.PLAIN, 14));
        JButton check = new JButton("Check Skill Gap");
        Runnable run = () -> {
            Job j = (Job) jobBox.getSelectedItem();
            if (j == null) { bar.setValue(0); bar.setString("0%"); result.setText("No opportunities available."); return; }
            SkillMatcher.Result r = SkillMatcher.compare(student().getSkills(), j.getRequiredSkills());
            bar.setValue(r.getPercent()); bar.setString(r.getPercent() + "% match");
            result.setText(skillGapText(j, r)); result.setCaretPosition(0);
        };
        jobBox.addActionListener(e -> run.run());
        check.addActionListener(e -> run.run());
        Runnable reload = () -> { fillJobBox(jobBox); run.run(); };

        JPanel top = new JPanel(new BorderLayout(6, 6));
        JPanel pick = new JPanel(new BorderLayout(6, 6));
        pick.add(new JLabel("Choose an opportunity:"), BorderLayout.WEST); pick.add(jobBox, BorderLayout.CENTER); pick.add(check, BorderLayout.EAST);
        top.add(pick, BorderLayout.NORTH); top.add(bar, BorderLayout.SOUTH);
        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(result), BorderLayout.CENTER);
        tabRefresh.put(panel, reload);
        reload.run();
        return panel;
    }

    private String skillGapText(Job j, SkillMatcher.Result r) {
        StringBuilder sb = new StringBuilder();
        sb.append("Opportunity: ").append(j.getTitle()).append(" at ").append(j.getCompany()).append("\n");
        sb.append("Your skills: ").append(student().getSkills().isEmpty() ? "(none yet - add them in My Profile)" : student().getSkillsCsv()).append("\n");
        sb.append("Required skills: ").append(j.getRequiredSkillsCsv()).append("\n\nMatched:\n");
        appendSkills(sb, r.getMatched(), "\u2713", "(none)");
        sb.append("\nMissing:\n");
        appendSkills(sb, r.getMissing(), "\u2717", "(nothing - you have every required skill)");
        sb.append("\nMatch percentage: ").append(r.getPercent()).append("%\n");
        if (!r.getMissing().isEmpty() && r.getTotalRequired() > 0)
            sb.append("Suggestion: learn ").append(String.join(", ", r.getMissing())).append(" to become a stronger candidate.\n");
        return sb.toString();
    }

    // =====================================================================
    //  STUDENT: Resume Checklist
    // =====================================================================
    private JPanel buildResumeTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JComboBox<Job> jobBox = new JComboBox<>();
        JCheckBox skillsBox = new JCheckBox("Required technical skills (checked automatically from your profile skills)");
        skillsBox.setEnabled(false);
        JLabel skillsHint = new JLabel(" ");
        JCheckBox projects = new JCheckBox("Resume has 1-2 projects relevant to this opportunity");
        JCheckBox experience = new JCheckBox("Resume has relevant experience (internship, club role, freelance, volunteering)");
        JCheckBox education = new JCheckBox("Education is listed (degree, college, expected graduation year)");
        JCheckBox contact = new JCheckBox("Contact information is listed (email, phone, LinkedIn / GitHub)");
        JTextArea result = new JTextArea(9, 40);
        result.setEditable(false); result.setFont(new Font("SansSerif", Font.PLAIN, 14));
        JButton evaluate = new JButton("Evaluate Resume");

        Runnable update = () -> {
            Job j = (Job) jobBox.getSelectedItem();
            if (j == null) { skillsBox.setSelected(false); skillsHint.setText(" "); return; }
            SkillMatcher.Result r = SkillMatcher.compare(student().getSkills(), j.getRequiredSkills());
            skillsBox.setSelected(r.getTotalRequired() > 0 && r.getMissing().isEmpty());
            skillsHint.setText(r.getMissing().isEmpty() ? "   All required skills found in your profile." : "   Missing from your profile: " + String.join(", ", r.getMissing()));
        };
        jobBox.addActionListener(e -> update.run());
        evaluate.addActionListener(e -> {
            Job j = (Job) jobBox.getSelectedItem();
            if (j == null) { showError("Choose an opportunity first."); return; }
            update.run();
            SkillMatcher.Result r = SkillMatcher.compare(student().getSkills(), j.getRequiredSkills());
            List<String> req = j.getRequiredSkills();
            String use = req.isEmpty() ? "the required skills" : String.join(", ", req.subList(0, Math.min(2, req.size())));
            JCheckBox[] boxes = {skillsBox, projects, experience, education, contact};
            String[] names = {"Required technical skills", "Relevant projects", "Relevant experience", "Education", "Contact information"};
            String[] advice = {"learn or add: " + String.join(", ", r.getMissing()),
                               "add a project that uses " + use,
                               "add an internship, club role or freelance work related to this " + j.getJobType().toLowerCase(Locale.ROOT),
                               "add your degree, college and expected graduation year",
                               "add your email, phone number and a LinkedIn / GitHub link"};
            int done = 0;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < boxes.length; i++) {
                if (boxes[i].isSelected()) { done++; sb.append("\u2713 ").append(names[i]).append("\n"); }
                else sb.append("\u2717 ").append(names[i]).append("  ->  ").append(advice[i]).append("\n");
            }
            result.setText("Resume checklist for " + j.getTitle() + " at " + j.getCompany() + "\nScore: " + done + "/5 (" + (done * 20) + "%)\n\n" + sb);
            result.setCaretPosition(0);
        });
        Runnable reload = () -> { fillJobBox(jobBox); update.run(); };

        JPanel pick = new JPanel(new BorderLayout(6, 6));
        pick.add(new JLabel("Choose an opportunity:"), BorderLayout.WEST); pick.add(jobBox, BorderLayout.CENTER);
        JPanel list = new JPanel(new GridLayout(0, 1, 2, 2));
        list.add(pick); list.add(skillsBox); list.add(skillsHint); list.add(projects); list.add(experience); list.add(education); list.add(contact);
        JPanel btn = new JPanel(new FlowLayout(FlowLayout.LEFT)); btn.add(evaluate);
        JPanel north = new JPanel(new BorderLayout(4, 4));
        north.add(list, BorderLayout.CENTER); north.add(btn, BorderLayout.SOUTH);
        panel.add(north, BorderLayout.NORTH);
        panel.add(new JScrollPane(result), BorderLayout.CENTER);
        tabRefresh.put(panel, reload);
        reload.run();
        return panel;
    }

    // =====================================================================
    //  STUDENT: Alumni referral request
    // =====================================================================
    private JPanel buildStudentReferralTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JComboBox<Job> jobBox = new JComboBox<>();
        JComboBox<AlumniChoice> alumniBox = new JComboBox<>();
        JLabel note = new JLabel(" ");
        JTextArea message = new JTextArea(3, 30);
        message.setLineWrap(true); message.setWrapStyleWord(true);
        JButton send = new JButton("Send Referral Request");
        DefaultTableModel model = newModel("Req ID", "Alumnus", "Company", "Job", "Status", "Sent On");
        JTable table = newTable(model);

        jobBox.addActionListener(e -> fillEligibleAlumni((Job) jobBox.getSelectedItem(), alumniBox, note));
        Runnable loadTable = () -> {
            model.setRowCount(0);
            try {
                for (Referral r : referralRepo.forStudent(currentUser.getId())) {
                    User alumnus = AppContext.users.findById(r.getAlumniId());
                    Job j = jobRepo.findById(r.getJobId());
                    model.addRow(new Object[]{r.getId(), alumnus == null ? "Alumnus #" + r.getAlumniId() : alumnus.getFullName(),
                        j == null ? "-" : j.getCompany(), j == null ? "Job #" + r.getJobId() : j.getTitle(), r.getStatus(), r.getCreatedAt()});
                }
            } catch (Exception ex) { showError("Could not load your referral requests: " + ex.getMessage()); }
        };
        Runnable reload = () -> { fillJobBox(jobBox); loadTable.run(); };
        send.addActionListener(e -> {
            Job j = (Job) jobBox.getSelectedItem();
            AlumniChoice a = (AlumniChoice) alumniBox.getSelectedItem();
            if (j == null) { showError("Please select a job."); return; }
            if (a == null) { showError("Please select an eligible alumnus."); return; }
            String text = message.getText().trim();
            if (text.isEmpty()) { showError("Please write a short message to the alumnus."); return; }
            try {
                referralRepo.create(currentUser.getId(), a.alumni.getId(), j.getId(), text);
                JOptionPane.showMessageDialog(this, "Referral request sent to " + a.alumni.getFullName() + ".");
                message.setText(""); loadTable.run();
            } catch (Exception ex) { showError("Could not send the request: " + ex.getMessage()); }
        });

        JPanel form = new JPanel(new GridBagLayout());
        addFormRow(form, 0, "1. Select a job:", jobBox);
        addFormRow(form, 1, "2. Eligible alumnus:", alumniBox);
        addFormRow(form, 2, "", note);
        addFormRow(form, 3, "3. Message:", new JScrollPane(message));
        addFormRow(form, 4, "", send);
        JPanel history = new JPanel(new BorderLayout(4, 4));
        history.add(new JLabel("Your referral requests"), BorderLayout.NORTH);
        history.add(new JScrollPane(table), BorderLayout.CENTER);
        JButton refresh = new JButton("Refresh Requests");
        refresh.addActionListener(e -> loadTable.run());
        history.add(refresh, BorderLayout.SOUTH);
        panel.add(form, BorderLayout.NORTH);
        panel.add(history, BorderLayout.CENTER);
        tabRefresh.put(panel, reload);
        reload.run();
        return panel;
    }

    /** Eligible alumni = work at the job's company, or share at least one required skill. Same company first. */
    private void fillEligibleAlumni(Job job, JComboBox<AlumniChoice> box, JLabel note) {
        box.removeAllItems();
        if (job == null) { note.setText(" "); return; }
        try {
            List<AlumniChoice> list = new ArrayList<>();
            for (Alumni a : AppContext.alumni.findAllAlumni()) {
                boolean same = a.getCompany().equalsIgnoreCase(job.getCompany());
                int shared = SkillMatcher.compare(a.getSkills(), job.getRequiredSkills()).getMatched().size();
                if (same || shared > 0) list.add(new AlumniChoice(a, same, shared));
            }
            Collections.sort(list, (x, y) -> {
                if (x.sameCompany != y.sameCompany) return x.sameCompany ? -1 : 1;
                if (x.shared != y.shared) return Integer.compare(y.shared, x.shared);
                return x.alumni.getFullName().compareToIgnoreCase(y.alumni.getFullName());
            });
            for (AlumniChoice c : list) box.addItem(c);
            note.setText(list.isEmpty() ? "No eligible alumni for this job (eligible = works at the company or shares a required skill)."
                : list.size() + " eligible alumni: same company first, then most shared skills.");
        } catch (Exception ex) { showError("Could not load alumni: " + ex.getMessage()); }
    }

    private static class AlumniChoice {
        final Alumni alumni; final boolean sameCompany; final int shared;
        AlumniChoice(Alumni alumni, boolean sameCompany, int shared) { this.alumni = alumni; this.sameCompany = sameCompany; this.shared = shared; }
        @Override public String toString() {
            return alumni.getFullName() + " - " + alumni.getJobRole() + " at " + alumni.getCompany()
                + (sameCompany ? "  [works at this company]" : "  [shares " + shared + " skill(s)]");
        }
    }

    // =====================================================================
    //  ALUMNI: Referral inbox (accept / reject)
    // =====================================================================
    private JPanel buildAlumniInboxTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        DefaultTableModel model = newModel("Req ID", "Student", "Job", "Message", "Status", "Received");
        JTable table = newTable(model);
        List<Referral> shown = new ArrayList<>();
        Runnable reload = () -> {
            model.setRowCount(0); shown.clear();
            try {
                for (Referral r : referralRepo.forAlumni(currentUser.getId())) {
                    shown.add(r);
                    User s = AppContext.users.findById(r.getStudentId());
                    Job j = jobRepo.findById(r.getJobId());
                    model.addRow(new Object[]{r.getId(), s == null ? "Student #" + r.getStudentId() : s.getFullName(),
                        j == null ? "Job #" + r.getJobId() : j.getTitle() + " (" + j.getCompany() + ")", r.getMessage(), r.getStatus(), r.getCreatedAt()});
                }
            } catch (Exception ex) { showError("Could not load the inbox: " + ex.getMessage()); }
        };
        JButton view = new JButton("View Request"), accept = new JButton("Accept"), reject = new JButton("Reject"), refresh = new JButton("Refresh Inbox");
        view.addActionListener(e -> { Referral r = selected(table, shown, "a referral request"); if (r != null) showReferral(r); });
        accept.addActionListener(e -> answerReferral(table, shown, Referral.ACCEPTED, reload));
        reject.addActionListener(e -> answerReferral(table, shown, Referral.REJECTED, reload));
        refresh.addActionListener(e -> reload.run());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(view); buttons.add(accept); buttons.add(reject); buttons.add(refresh);
        panel.add(new JLabel("Referral requests sent to you by students:"), BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        tabRefresh.put(panel, reload);
        reload.run();
        return panel;
    }

    private void answerReferral(JTable table, List<Referral> shown, String status, Runnable reload) {
        Referral r = selected(table, shown, "a referral request");
        if (r == null) return;
        if (!Referral.PENDING.equals(r.getStatus())) { showError("Only pending requests can be answered."); return; }
        try {
            referralRepo.updateStatus(r.getId(), currentUser.getId(), status);
            JOptionPane.showMessageDialog(this, "Request " + status.toLowerCase(Locale.ROOT) + ".");
            reload.run();
        } catch (Exception ex) { showError("Could not update the request: " + ex.getMessage()); }
    }

    private void showReferral(Referral r) {
        try {
            User s = AppContext.users.findById(r.getStudentId());
            Job j = jobRepo.findById(r.getJobId());
            StringBuilder sb = new StringBuilder();
            sb.append("Student: ").append(s == null ? "#" + r.getStudentId() : s.getFullName()).append("\n");
            if (s != null) sb.append("Profile: ").append(s.getSummary()).append("\nSkills: ").append(s.getSkills().isEmpty() ? "(none listed)" : s.getSkillsCsv()).append("\n");
            if (j != null) {
                sb.append("\nJob: ").append(j.getTitle()).append(" at ").append(j.getCompany()).append("\nRequired skills: ").append(j.getRequiredSkillsCsv()).append("\n");
                if (s != null) sb.append("Student's skill match: ").append(SkillMatcher.compare(s.getSkills(), j.getRequiredSkills()).getPercent()).append("%\n");
            }
            sb.append("\nMessage:\n").append(r.getMessage()).append("\n\nStatus: ").append(r.getStatus()).append("\nReceived: ").append(r.getCreatedAt());
            showText("Referral Request #" + r.getId(), sb.toString());
        } catch (Exception ex) { showError("Could not open the request: " + ex.getMessage()); }
    }

    // =====================================================================
    //  ALUMNI: Post a job
    // =====================================================================
    private JPanel buildPostJobTab() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        JTextField title = new JTextField(30), company = new JTextField(((Alumni) currentUser).getCompany(), 30),
                   location = new JTextField(30), skills = new JTextField(30);
        JComboBox<String> type = new JComboBox<>(new String[]{Job.JOB, Job.INTERNSHIP});
        JTextArea desc = new JTextArea(4, 30);
        desc.setLineWrap(true); desc.setWrapStyleWord(true);
        JButton post = new JButton("Post Opportunity");
        post.addActionListener(e -> {
            if (title.getText().trim().isEmpty() || company.getText().trim().isEmpty()) { showError("Title and company are required."); return; }
            if (User.split(skills.getText()).isEmpty()) { showError("Enter at least one required skill, separated by commas."); return; }
            try {
                jobRepo.add(new Job(0, title.getText(), company.getText(), (String) type.getSelectedItem(), location.getText(),
                    skills.getText(), desc.getText(), currentUser.getId(), null));
                JOptionPane.showMessageDialog(this, "Opportunity posted. Students can now see it.");
                title.setText(""); location.setText(""); skills.setText(""); desc.setText("");
            } catch (Exception ex) { showError("Could not post the opportunity: " + ex.getMessage()); }
        });
        JPanel form = new JPanel(new GridBagLayout());
        addFormRow(form, 0, "Title:", title);
        addFormRow(form, 1, "Company:", company);
        addFormRow(form, 2, "Type:", type);
        addFormRow(form, 3, "Location:", location);
        addFormRow(form, 4, "Required skills (comma separated):", skills);
        addFormRow(form, 5, "Description:", new JScrollPane(desc));
        addFormRow(form, 6, "", post);
        panel.add(form, BorderLayout.NORTH);
        return panel;
    }

    // =====================================================================
    //  Shared actions and dialogs
    // =====================================================================
    private int matchPercent(Job j) {
        return SkillMatcher.compare(student().getSkills(), j.getRequiredSkills()).getPercent();
    }

    private void markJob(Job j, String action, Runnable after) {
        try { jobRepo.setAction(currentUser.getId(), j.getId(), action); after.run(); }
        catch (Exception ex) { showError("Could not update the opportunity: " + ex.getMessage()); }
    }

    private void applyTo(Job j) {
        if (JOptionPane.showConfirmDialog(this, "Apply to " + j.getTitle() + " at " + j.getCompany() + "?\nYour match score: " + matchPercent(j) + "%",
                TITLE, JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
        try {
            appRepo.apply(currentUser.getId(), j.getId());
            JOptionPane.showMessageDialog(this, "Application submitted. Track it in the My Applications tab.");
        } catch (Exception ex) { showError("Could not apply: " + ex.getMessage()); }
    }

    private void showDetails(Job j) {
        try {
            User poster = j.getPostedBy() > 0 ? AppContext.users.findById(j.getPostedBy()) : null;
            showText("Opportunity Details",
                j.getTitle() + "\n" + j.getCompany() + "  |  " + j.getJobType() + "  |  " + j.getLocation()
                + "\n\nRequired skills: " + j.getRequiredSkillsCsv()
                + "\n\nDescription:\n" + (j.getDescription().isEmpty() ? "(none)" : j.getDescription())
                + "\n\nPosted by: " + (poster == null ? "Placement cell" : poster.getFullName())
                + (j.getCreatedAt().isEmpty() ? "" : "\nPosted on: " + j.getCreatedAt())
                + "\nYour match score: " + matchPercent(j) + "%");
        } catch (Exception ex) { showError("Could not open the details: " + ex.getMessage()); }
    }

    /** "Why am I a match?" - score, matched skills, missing skills and a plain explanation. */
    private void showMatch(Job j) {
        SkillMatcher.Result r = SkillMatcher.compare(student().getSkills(), j.getRequiredSkills());
        StringBuilder sb = new StringBuilder();
        sb.append("Why am I a match for ").append(j.getTitle()).append(" at ").append(j.getCompany()).append("?\n\n");
        sb.append("Match Score: ").append(r.getPercent()).append("%\n\nYou match:\n");
        appendSkills(sb, r.getMatched(), "\u2713", "(no matching skills yet)");
        sb.append("\nMissing:\n");
        appendSkills(sb, r.getMissing(), "\u2717", "(nothing - you have every required skill)");
        sb.append("\nExplanation:\n").append(r.getExplanation());
        showText("Why am I a match?", sb.toString());
    }

    private void showApplication(Application a) {
        try {
            Job j = jobRepo.findById(a.getJobId());
            StringBuilder sb = new StringBuilder();
            sb.append("Application #").append(a.getId()).append("\nStatus: ").append(a.getStatus()).append("\nApplied on: ").append(a.getAppliedAt())
              .append("\nNotes: ").append(a.getNotes().isEmpty() ? "(none)" : a.getNotes()).append("\n\n");
            if (j == null) sb.append("Job: ").append(a.getJobTitle()).append(" at ").append(a.getCompany()).append(" (listing removed)");
            else sb.append("Job: ").append(j.getTitle()).append(" at ").append(j.getCompany()).append("\nType: ").append(j.getJobType())
                   .append("\nLocation: ").append(j.getLocation()).append("\nRequired skills: ").append(j.getRequiredSkillsCsv())
                   .append("\nYour match score: ").append(matchPercent(j)).append("%");
            showText("Application Details", sb.toString());
        } catch (Exception ex) { showError("Could not open the application: " + ex.getMessage()); }
    }

    private void appendSkills(StringBuilder sb, List<String> skills, String mark, String empty) {
        if (skills.isEmpty()) sb.append("  ").append(empty).append("\n");
        for (String s : skills) sb.append("  ").append(mark).append(' ').append(s).append("\n");
    }

    // =====================================================================
    //  Small Swing helpers
    // =====================================================================
    private DefaultTableModel newModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
    }

    private JTable newTable(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setRowHeight(22);
        t.getTableHeader().setReorderingAllowed(false);
        return t;
    }

    private <T> T selected(JTable table, List<T> shown, String what) {
        int row = table.getSelectedRow();
        if (row < 0 || row >= shown.size()) { showError("Select " + what + " first."); return null; }
        return shown.get(row);
    }

    /** Refills a job drop-down but keeps the current selection when possible. */
    private void fillJobBox(JComboBox<Job> box) {
        Job old = (Job) box.getSelectedItem();
        int oldId = old == null ? -1 : old.getId();
        box.removeAllItems();
        try {
            Job pick = null;
            for (Job j : jobRepo.findAll()) { box.addItem(j); if (j.getId() == oldId) pick = j; }
            if (pick != null) box.setSelectedItem(pick);
        } catch (Exception ex) { showError("Could not load opportunities: " + ex.getMessage()); }
    }

    private void addFormRow(JPanel form, int row, String label, Component field) {
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 5, 5, 5); g.fill = GridBagConstraints.HORIZONTAL; g.gridy = row;
        g.gridx = 0; g.weightx = 0; form.add(new JLabel(label), g);
        g.gridx = 1; g.weightx = 1; form.add(field, g);
    }

    private void showText(String title, String text) {
        JTextArea ta = new JTextArea(text);
        ta.setEditable(false); ta.setLineWrap(true); ta.setWrapStyleWord(true);
        ta.setFont(new Font("SansSerif", Font.PLAIN, 14));
        ta.setCaretPosition(0);
        JScrollPane sp = new JScrollPane(ta);
        sp.setPreferredSize(new Dimension(480, 320));
        JOptionPane.showMessageDialog(this, sp, title, JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String message) { JOptionPane.showMessageDialog(this, message, TITLE, JOptionPane.WARNING_MESSAGE); }
}
