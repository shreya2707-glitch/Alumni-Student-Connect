import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.*;
import java.util.List;

/** Interactive student/alumni workflow for mentorship, roadmaps, and career quests. */
public class MentorshipPanel extends JPanel {
    private final User currentUser;
    private final MentorshipRepository repository;
    private final JTabbedPane innerTabs = new JTabbedPane();

    public MentorshipPanel(User currentUser) {
        this.currentUser = currentUser;
        this.repository = new MentorshipRepository();
        setLayout(new BorderLayout(8,8));
        setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
        JLabel heading = new JLabel("Mentorship & Career Quest  |  Build connections. Track your growth.");
        heading.setFont(new Font("SansSerif",Font.BOLD,17));
        add(heading,BorderLayout.NORTH);
        if (currentUser instanceof Student) {
            innerTabs.addTab("Find a Mentor", buildFindMentorTab());
            innerTabs.addTab("My Requests", buildStudentRequestsTab());
            innerTabs.addTab("Career Roadmap", buildRoadmapTab());
            innerTabs.addTab("Career Quests", buildQuestsTab());
        } else if (currentUser instanceof Alumni) {
            innerTabs.addTab("Mentorship Inbox", buildAlumniInboxTab());
        }
        add(innerTabs,BorderLayout.CENTER);
    }

    private JPanel buildFindMentorTab() {
        JPanel panel = new JPanel(new BorderLayout(8,8));
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints(); g.insets=new Insets(5,5,5,5); g.fill=GridBagConstraints.HORIZONTAL;
        JComboBox<Alumni> alumniBox = new JComboBox<>();
        JTextArea message = new JTextArea(4,30); message.setLineWrap(true); message.setWrapStyleWord(true);
        JTextArea recommendations = new JTextArea(); recommendations.setEditable(false); recommendations.setLineWrap(true); recommendations.setWrapStyleWord(true);
        JScrollPane recommendationScroll = new JScrollPane(recommendations);
        try {
            List<Alumni> all=AppContext.alumni.findAllAlumni();
            Student student=(Student)currentUser;
            all.sort((a,b)->Integer.compare(AppContext.matcher.score(student,b),AppContext.matcher.score(student,a)));
            for(Alumni a:all) alumniBox.addItem(a);
            StringBuilder text=new StringBuilder("Recommended mentors based on your profile:\n\n");
            for(int i=0;i<Math.min(5,all.size());i++) {
                Alumni a=all.get(i);
                text.append(i+1).append(". ").append(a.getFullName()).append(" — ").append(a.getJobRole()).append(" at ").append(a.getCompany())
                    .append(" | Match: ").append(AppContext.matcher.score(student,a)).append("%\n")
                    .append("   Skills: ").append(a.getSkillsCsv()).append("\n\n");
            }
            if(all.isEmpty()) text.append("No alumni profiles are available yet.");
            recommendations.setText(text.toString());
        } catch(Exception ex) { recommendations.setText("Could not load mentor recommendations: "+ex.getMessage()); }
        g.gridx=0;g.gridy=0;g.weightx=0;form.add(new JLabel("Choose an alumnus:"),g);
        g.gridx=1;g.weightx=1;form.add(alumniBox,g);
        g.gridx=0;g.gridy=1;g.weightx=0;form.add(new JLabel("Message:"),g);
        g.gridx=1;g.weightx=1;form.add(new JScrollPane(message),g);
        JButton send=new JButton("Send Mentorship Request");
        send.addActionListener(e->{
            Alumni selected=(Alumni)alumniBox.getSelectedItem();
            if(selected==null) { showError("Please select an alumnus."); return; }
            String note=message.getText().trim();
            if(note.isEmpty()) { showError("Please write a short message explaining what guidance you need."); return; }
            try {
                repository.createRequest(currentUser.getId(),selected.getId(),note);
                JOptionPane.showMessageDialog(this,"Mentorship request sent to "+selected.getFullName()+".");
                message.setText("");
            } catch(Exception ex) { showError("Could not send request: "+ex.getMessage()); }
        });
        g.gridx=1;g.gridy=2;g.weightx=1;form.add(send,g);
        panel.add(form,BorderLayout.NORTH);
        JPanel recPanel=new JPanel(new BorderLayout(4,4)); recPanel.add(new JLabel("Mentor recommendations"),BorderLayout.NORTH); recPanel.add(recommendationScroll,BorderLayout.CENTER);
        panel.add(recPanel,BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildStudentRequestsTab() {
        JPanel panel=new JPanel(new BorderLayout(8,8));
        DefaultListModel<String> model=new DefaultListModel<>(); JList<String> list=new JList<>(model);
        JButton refresh=new JButton("Refresh Requests");
        Runnable reload=()->{
            model.clear();
            try {
                for(MentorshipRepository.MentorRequest r:repository.requestsForStudent(currentUser.getId())) {
                    User alumnus=AppContext.users.findById(r.alumniId);
                    String name=alumnus==null?"Alumnus #"+r.alumniId:alumnus.getFullName();
                    model.addElement("#"+r.id+" | "+name+" | "+r.status+" | "+r.message);
                }
                if(model.isEmpty()) model.addElement("No mentorship requests yet. Use Find a Mentor to send one.");
            } catch(Exception ex) { model.addElement("Could not load requests: "+ex.getMessage()); }
        };
        refresh.addActionListener(e->reload.run());
        panel.add(new JScrollPane(list),BorderLayout.CENTER); panel.add(refresh,BorderLayout.SOUTH); reload.run();
        return panel;
    }

    private JPanel buildAlumniInboxTab() {
        JPanel panel=new JPanel(new BorderLayout(8,8));
        DefaultListModel<MentorshipRepository.MentorRequest> model=new DefaultListModel<>();
        JList<MentorshipRepository.MentorRequest> list=new JList<>(model);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> l,Object value,int index,boolean selected,boolean focus) {
                MentorshipRepository.MentorRequest r=(MentorshipRepository.MentorRequest)value;
                User student=AppContext.users.findById(r.studentId);
                String name=student==null?"Student #"+r.studentId:student.getFullName();
                return super.getListCellRendererComponent(l,"#"+r.id+" | "+name+" | "+r.status+" | "+r.message,index,selected,focus);
            }
        });
        JButton accept=new JButton("Accept"), decline=new JButton("Decline"), refresh=new JButton("Refresh Inbox");
        Runnable reload=()->{
            model.clear();
            try { for(MentorshipRepository.MentorRequest r:repository.requestsForAlumni(currentUser.getId())) model.addElement(r);
                if(model.isEmpty()) JOptionPane.showMessageDialog(this,"Your inbox is empty.");
            } catch(Exception ex) { showError("Could not load inbox: "+ex.getMessage()); }
        };
        accept.addActionListener(e->updateSelectedRequest(list,"ACCEPTED",reload));
        decline.addActionListener(e->updateSelectedRequest(list,"DECLINED",reload));
        refresh.addActionListener(e->reload.run());
        JPanel buttons=new JPanel(new FlowLayout(FlowLayout.LEFT)); buttons.add(accept);buttons.add(decline);buttons.add(refresh);
        panel.add(new JScrollPane(list),BorderLayout.CENTER);panel.add(buttons,BorderLayout.SOUTH);
        // Initial load does not show an empty-inbox popup on first launch.
        try { for(MentorshipRepository.MentorRequest r:repository.requestsForAlumni(currentUser.getId())) model.addElement(r); }
        catch(Exception ex) { showError("Could not load inbox: "+ex.getMessage()); }
        return panel;
    }

    private void updateSelectedRequest(JList<MentorshipRepository.MentorRequest> list,String status,Runnable reload) {
        MentorshipRepository.MentorRequest selected=list.getSelectedValue();
        if(selected==null) { showError("Select a mentorship request first."); return; }
        if(!"PENDING".equals(selected.status)) { showError("Only pending requests can be updated."); return; }
        try {
            repository.updateRequestStatus(selected.id,currentUser.getId(),status);
            JOptionPane.showMessageDialog(this,"Request "+status.toLowerCase(Locale.ROOT)+"."); reload.run();
        } catch(Exception ex) { showError("Could not update request: "+ex.getMessage()); }
    }

    private JPanel buildRoadmapTab() {
        JPanel panel=new JPanel(new BorderLayout(8,8));
        DefaultComboBoxModel<MentorshipRepository.Roadmap> roadmapModel=new DefaultComboBoxModel<>();
        JComboBox<MentorshipRepository.Roadmap> roadmapBox=new JComboBox<>(roadmapModel);
        DefaultListModel<MentorshipRepository.Milestone> milestoneModel=new DefaultListModel<>();
        JList<MentorshipRepository.Milestone> milestoneList=new JList<>(milestoneModel);
        JButton create=new JButton("Create Roadmap"), addMilestone=new JButton("Add Milestone"), toggle=new JButton("Mark Complete / Incomplete"), refresh=new JButton("Refresh");
        Runnable loadRoadmaps=()->{
            roadmapModel.removeAllElements();
            try { for(MentorshipRepository.Roadmap r:repository.roadmapsForStudent(currentUser.getId())) roadmapModel.addElement(r); }
            catch(Exception ex) { showError("Could not load roadmaps: "+ex.getMessage()); }
            loadMilestones(roadmapBox,milestoneModel);
        };
        roadmapBox.addActionListener(e->loadMilestones(roadmapBox,milestoneModel));
        create.addActionListener(e->{
            JTextField title=new JTextField(), role=new JTextField(((Student)currentUser).getTargetIndustry());
            JPanel fields=new JPanel(new GridLayout(0,1,4,4));fields.add(new JLabel("Roadmap title:"));fields.add(title);fields.add(new JLabel("Target role:"));fields.add(role);
            if(JOptionPane.showConfirmDialog(this,fields,"Create Career Roadmap",JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION) {
                if(title.getText().trim().isEmpty()||role.getText().trim().isEmpty()) {showError("Enter both a roadmap title and target role.");return;}
                try { repository.createRoadmap(currentUser.getId(),title.getText().trim(),role.getText().trim());loadRoadmaps.run(); }
                catch(Exception ex) {showError("Could not create roadmap: "+ex.getMessage());}
            }
        });
        addMilestone.addActionListener(e->{
            MentorshipRepository.Roadmap r=(MentorshipRepository.Roadmap)roadmapBox.getSelectedItem();
            if(r==null) {showError("Create or select a roadmap first.");return;}
            JTextField title=new JTextField(); JTextArea desc=new JTextArea(3,25);
            JPanel fields=new JPanel(new GridLayout(0,1,4,4));fields.add(new JLabel("Milestone title:"));fields.add(title);fields.add(new JLabel("Description:"));fields.add(new JScrollPane(desc));
            if(JOptionPane.showConfirmDialog(this,fields,"Add Roadmap Milestone",JOptionPane.OK_CANCEL_OPTION)==JOptionPane.OK_OPTION) {
                if(title.getText().trim().isEmpty()) {showError("Milestone title is required.");return;}
                try {repository.addMilestone(r.id,title.getText().trim(),desc.getText().trim());loadMilestones(roadmapBox,milestoneModel);}
                catch(Exception ex) {showError("Could not add milestone: "+ex.getMessage());}
            }
        });
        toggle.addActionListener(e->{
            MentorshipRepository.Milestone m=milestoneList.getSelectedValue();
            if(m==null) {showError("Select a milestone first.");return;}
            try {repository.toggleMilestone(m.id,!m.completed);loadMilestones(roadmapBox,milestoneModel);}
            catch(Exception ex) {showError("Could not update milestone: "+ex.getMessage());}
        });
        refresh.addActionListener(e->loadRoadmaps.run());
        JPanel top=new JPanel(new BorderLayout(5,5));top.add(new JLabel("Your roadmap:"),BorderLayout.WEST);top.add(roadmapBox,BorderLayout.CENTER);
        JPanel buttons=new JPanel(new FlowLayout(FlowLayout.LEFT));buttons.add(create);buttons.add(addMilestone);buttons.add(toggle);buttons.add(refresh);
        panel.add(top,BorderLayout.NORTH);panel.add(new JScrollPane(milestoneList),BorderLayout.CENTER);panel.add(buttons,BorderLayout.SOUTH);
        loadRoadmaps.run(); return panel;
    }

    private void loadMilestones(JComboBox<MentorshipRepository.Roadmap> roadmapBox,DefaultListModel<MentorshipRepository.Milestone> model) {
        model.clear(); MentorshipRepository.Roadmap selected=(MentorshipRepository.Roadmap)roadmapBox.getSelectedItem();
        if(selected==null) return;
        try {for(MentorshipRepository.Milestone m:repository.milestonesForRoadmap(selected.id)) model.addElement(m);}
        catch(Exception ex) {showError("Could not load milestones: "+ex.getMessage());}
    }

    private JPanel buildQuestsTab() {
        JPanel panel=new JPanel(new BorderLayout(8,8));
        DefaultListModel<MentorshipRepository.Quest> model=new DefaultListModel<>();JList<MentorshipRepository.Quest> list=new JList<>(model);
        JTextArea detail=new JTextArea(4,30);detail.setEditable(false);detail.setLineWrap(true);detail.setWrapStyleWord(true);
        JLabel progress=new JLabel("Complete quests to earn points and badges.");
        JButton action=new JButton("Start / Complete Selected Quest"),refresh=new JButton("Refresh Quests");
        list.addListSelectionListener(e->{if(!e.getValueIsAdjusting()) {MentorshipRepository.Quest q=list.getSelectedValue();if(q!=null) {try {detail.setText(q.description+"\n\nStatus: "+repository.questStatus(currentUser.getId(),q.id)+"\nPoints: "+q.points);}catch(Exception ex){detail.setText(ex.getMessage());}}}});
        Runnable reload=()->{
            model.clear();
            try {for(MentorshipRepository.Quest q:repository.allQuests()) model.addElement(q);
                int completed=repository.completedQuestCount(currentUser.getId());
                String badge=completed>=5?"Career Champion":completed>=3?"Quest Explorer":completed>=1?"First Step":"No badge yet";
                progress.setText("Completed quests: "+completed+" / "+model.size()+"     |     Badge: "+badge);
            } catch(Exception ex) {showError("Could not load quests: "+ex.getMessage());}
        };
        action.addActionListener(e->{MentorshipRepository.Quest q=list.getSelectedValue();if(q==null){showError("Select a quest first.");return;}
            try {String status=repository.questStatus(currentUser.getId(),q.id);if(status.equals("COMPLETED")){JOptionPane.showMessageDialog(this,"This quest is already completed.");return;}repository.advanceQuest(currentUser.getId(),q.id);reload.run();detail.setText(q.description+"\n\nNew status: "+repository.questStatus(currentUser.getId(),q.id)+"\nPoints: "+q.points);}
            catch(Exception ex){showError("Could not update quest: "+ex.getMessage());}
        });
        refresh.addActionListener(e->reload.run());
        JPanel bottom=new JPanel(new BorderLayout(5,5));bottom.add(new JScrollPane(detail),BorderLayout.CENTER);bottom.add(progress,BorderLayout.NORTH);
        JPanel buttons=new JPanel(new FlowLayout(FlowLayout.LEFT));buttons.add(action);buttons.add(refresh);bottom.add(buttons,BorderLayout.SOUTH);
        panel.add(new JScrollPane(list),BorderLayout.CENTER);panel.add(bottom,BorderLayout.SOUTH);reload.run();return panel;
    }

    private void showError(String message) { JOptionPane.showMessageDialog(this,message,"Mentorship & Career Quest",JOptionPane.WARNING_MESSAGE); }
}
