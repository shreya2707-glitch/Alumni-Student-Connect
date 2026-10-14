import java.sql.*;
import java.util.*;

/** Data access for Member 2's tables. Uses MySQL when available and memory otherwise. */
public class MentorshipRepository {
    private static final List<MentorRequest> memoryRequests = new ArrayList<>();
    private static final List<Roadmap> memoryRoadmaps = new ArrayList<>();
    private static final List<Milestone> memoryMilestones = new ArrayList<>();
    private static final List<Quest> memoryQuests = new ArrayList<>();
    private static final Map<String, String> memoryStudentQuests = new HashMap<>();
    private static int nextRequestId = 1, nextRoadmapId = 1, nextMilestoneId = 1;
    private static boolean memorySeeded = false;

    public MentorshipRepository() {
        if (AppContext.usingDatabase) {
            try (Connection c = DBConnection.get(); Statement s = c.createStatement()) {
                for (String sql : tableSql()) s.execute(sql);
                seedDatabaseQuests(c);
            } catch (SQLException ex) {
                throw new IllegalStateException("Could not prepare mentorship tables: " + ex.getMessage(), ex);
            }
        } else {
            seedMemoryQuests();
        }
    }

    private List<String> tableSql() {
        List<String> sql = new ArrayList<>();
        sql.add("CREATE TABLE IF NOT EXISTS mentorship_requests (id INT PRIMARY KEY AUTO_INCREMENT, student_id INT NOT NULL, alumni_id INT NOT NULL, message VARCHAR(500) NOT NULL, status ENUM('PENDING','ACCEPTED','DECLINED') NOT NULL DEFAULT 'PENDING', created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE, FOREIGN KEY (alumni_id) REFERENCES users(id) ON DELETE CASCADE)");
        sql.add("CREATE TABLE IF NOT EXISTS career_roadmaps (id INT PRIMARY KEY AUTO_INCREMENT, student_id INT NOT NULL, title VARCHAR(150) NOT NULL, target_role VARCHAR(120) NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE)");
        sql.add("CREATE TABLE IF NOT EXISTS roadmap_milestones (id INT PRIMARY KEY AUTO_INCREMENT, roadmap_id INT NOT NULL, title VARCHAR(150) NOT NULL, description VARCHAR(500), is_completed BOOLEAN NOT NULL DEFAULT FALSE, FOREIGN KEY (roadmap_id) REFERENCES career_roadmaps(id) ON DELETE CASCADE)");
        sql.add("CREATE TABLE IF NOT EXISTS career_quests (id INT PRIMARY KEY AUTO_INCREMENT, title VARCHAR(150) NOT NULL UNIQUE, description VARCHAR(500) NOT NULL, points INT NOT NULL DEFAULT 10)");
        sql.add("CREATE TABLE IF NOT EXISTS student_quests (student_id INT NOT NULL, quest_id INT NOT NULL, status ENUM('IN_PROGRESS','COMPLETED') NOT NULL DEFAULT 'IN_PROGRESS', completed_at TIMESTAMP NULL, PRIMARY KEY(student_id, quest_id), FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE, FOREIGN KEY (quest_id) REFERENCES career_quests(id) ON DELETE CASCADE)");
        return sql;
    }

    private void seedDatabaseQuests(Connection c) throws SQLException {
        String[][] seed = {
            {"Polish Your Resume", "Create or improve a one-page resume.", "10"},
            {"Build a Portfolio Project", "Complete a small project related to your target role.", "20"},
            {"Practise an Interview", "Practise answering five common interview questions.", "15"},
            {"Connect with a Professional", "Prepare three thoughtful questions for an alumnus.", "10"},
            {"Learn a New Skill", "Finish one beginner lesson in a useful technical skill.", "15"}
        };
        try (PreparedStatement check = c.prepareStatement("SELECT COUNT(*) FROM career_quests"); ResultSet rs = check.executeQuery()) {
            rs.next(); if (rs.getInt(1) > 0) return;
        }
        try (PreparedStatement p = c.prepareStatement("INSERT INTO career_quests(title,description,points) VALUES(?,?,?)")) {
            for (String[] q : seed) { p.setString(1,q[0]); p.setString(2,q[1]); p.setInt(3,Integer.parseInt(q[2])); p.addBatch(); }
            p.executeBatch();
        }
    }

    private void seedMemoryQuests() {
        if (memorySeeded) return;
        memoryQuests.add(new Quest(1,"Polish Your Resume","Create or improve a one-page resume.",10));
        memoryQuests.add(new Quest(2,"Build a Portfolio Project","Complete a small project related to your target role.",20));
        memoryQuests.add(new Quest(3,"Practise an Interview","Practise answering five common interview questions.",15));
        memoryQuests.add(new Quest(4,"Connect with a Professional","Prepare three thoughtful questions for an alumnus.",10));
        memoryQuests.add(new Quest(5,"Learn a New Skill","Finish one beginner lesson in a useful technical skill.",15));
        memorySeeded = true;
    }

    public void createRequest(int studentId, int alumniId, String message) throws SQLException {
        if (studentId == alumniId) throw new IllegalArgumentException("You cannot request yourself as a mentor.");
        if (AppContext.usingDatabase) {
            String q = "INSERT INTO mentorship_requests(student_id,alumni_id,message) VALUES(?,?,?)";
            try (Connection c=DBConnection.get(); PreparedStatement p=c.prepareStatement(q)) {
                p.setInt(1,studentId); p.setInt(2,alumniId); p.setString(3,message); p.executeUpdate();
            }
        } else {
            memoryRequests.add(new MentorRequest(nextRequestId++,studentId,alumniId,message,"PENDING",""));
        }
    }

    public List<MentorRequest> requestsForStudent(int studentId) throws SQLException {
        if (!AppContext.usingDatabase) {
            List<MentorRequest> out=new ArrayList<>(); for(MentorRequest r:memoryRequests) if(r.studentId==studentId) out.add(r); return out;
        }
        return readRequests("SELECT id,student_id,alumni_id,message,status,CAST(created_at AS CHAR) created_at FROM mentorship_requests WHERE student_id=? ORDER BY id DESC",studentId);
    }

    public List<MentorRequest> requestsForAlumni(int alumniId) throws SQLException {
        if (!AppContext.usingDatabase) {
            List<MentorRequest> out=new ArrayList<>(); for(MentorRequest r:memoryRequests) if(r.alumniId==alumniId) out.add(r); return out;
        }
        return readRequests("SELECT id,student_id,alumni_id,message,status,CAST(created_at AS CHAR) created_at FROM mentorship_requests WHERE alumni_id=? ORDER BY id DESC",alumniId);
    }

    private List<MentorRequest> readRequests(String sql,int userId) throws SQLException {
        List<MentorRequest> out=new ArrayList<>();
        try(Connection c=DBConnection.get(); PreparedStatement p=c.prepareStatement(sql)) {
            p.setInt(1,userId); try(ResultSet r=p.executeQuery()) { while(r.next()) out.add(new MentorRequest(r.getInt("id"),r.getInt("student_id"),r.getInt("alumni_id"),r.getString("message"),r.getString("status"),r.getString("created_at"))); }
        }
        return out;
    }

    public void updateRequestStatus(int requestId,int alumniId,String status) throws SQLException {
        if (!status.equals("ACCEPTED") && !status.equals("DECLINED")) throw new IllegalArgumentException("Invalid request status.");
        if (!AppContext.usingDatabase) {
            for(MentorRequest r:memoryRequests) if(r.id==requestId && r.alumniId==alumniId) { r.status=status; return; }
            throw new IllegalArgumentException("Request not found.");
        }
        try(Connection c=DBConnection.get(); PreparedStatement p=c.prepareStatement("UPDATE mentorship_requests SET status=? WHERE id=? AND alumni_id=? AND status='PENDING'")) {
            p.setString(1,status); p.setInt(2,requestId); p.setInt(3,alumniId);
            if(p.executeUpdate()==0) throw new IllegalArgumentException("Pending request not found.");
        }
    }

    public int createRoadmap(int studentId,String title,String targetRole) throws SQLException {
        if (!AppContext.usingDatabase) {
            Roadmap r=new Roadmap(nextRoadmapId++,studentId,title,targetRole); memoryRoadmaps.add(r); return r.id;
        }
        try(Connection c=DBConnection.get(); PreparedStatement p=c.prepareStatement("INSERT INTO career_roadmaps(student_id,title,target_role) VALUES(?,?,?)",Statement.RETURN_GENERATED_KEYS)) {
            p.setInt(1,studentId); p.setString(2,title); p.setString(3,targetRole); p.executeUpdate();
            try(ResultSet keys=p.getGeneratedKeys()) { if(keys.next()) return keys.getInt(1); }
        }
        throw new SQLException("Could not create roadmap.");
    }

    public List<Roadmap> roadmapsForStudent(int studentId) throws SQLException {
        if (!AppContext.usingDatabase) { List<Roadmap> out=new ArrayList<>(); for(Roadmap r:memoryRoadmaps) if(r.studentId==studentId) out.add(r); return out; }
        List<Roadmap> out=new ArrayList<>();
        try(Connection c=DBConnection.get(); PreparedStatement p=c.prepareStatement("SELECT id,student_id,title,target_role FROM career_roadmaps WHERE student_id=? ORDER BY id DESC")) {
            p.setInt(1,studentId); try(ResultSet r=p.executeQuery()) { while(r.next()) out.add(new Roadmap(r.getInt("id"),r.getInt("student_id"),r.getString("title"),r.getString("target_role"))); }
        }
        return out;
    }

    public void addMilestone(int roadmapId,String title,String description) throws SQLException {
        if (!AppContext.usingDatabase) { memoryMilestones.add(new Milestone(nextMilestoneId++,roadmapId,title,description,false)); return; }
        try(Connection c=DBConnection.get(); PreparedStatement p=c.prepareStatement("INSERT INTO roadmap_milestones(roadmap_id,title,description) VALUES(?,?,?)")) {
            p.setInt(1,roadmapId); p.setString(2,title); p.setString(3,description); p.executeUpdate();
        }
    }

    public List<Milestone> milestonesForRoadmap(int roadmapId) throws SQLException {
        if (!AppContext.usingDatabase) { List<Milestone> out=new ArrayList<>(); for(Milestone m:memoryMilestones) if(m.roadmapId==roadmapId) out.add(m); return out; }
        List<Milestone> out=new ArrayList<>();
        try(Connection c=DBConnection.get(); PreparedStatement p=c.prepareStatement("SELECT id,roadmap_id,title,description,is_completed FROM roadmap_milestones WHERE roadmap_id=? ORDER BY id")) {
            p.setInt(1,roadmapId); try(ResultSet r=p.executeQuery()) { while(r.next()) out.add(new Milestone(r.getInt("id"),r.getInt("roadmap_id"),r.getString("title"),r.getString("description"),r.getBoolean("is_completed"))); }
        }
        return out;
    }

    public void toggleMilestone(int milestoneId,boolean completed) throws SQLException {
        if (!AppContext.usingDatabase) { for(Milestone m:memoryMilestones) if(m.id==milestoneId) {m.completed=completed;return;} return; }
        try(Connection c=DBConnection.get(); PreparedStatement p=c.prepareStatement("UPDATE roadmap_milestones SET is_completed=? WHERE id=?")) { p.setBoolean(1,completed); p.setInt(2,milestoneId); p.executeUpdate(); }
    }

    public List<Quest> allQuests() throws SQLException {
        if (!AppContext.usingDatabase) return new ArrayList<>(memoryQuests);
        List<Quest> out=new ArrayList<>();
        try(Connection c=DBConnection.get(); PreparedStatement p=c.prepareStatement("SELECT id,title,description,points FROM career_quests ORDER BY id"); ResultSet r=p.executeQuery()) {
            while(r.next()) out.add(new Quest(r.getInt("id"),r.getString("title"),r.getString("description"),r.getInt("points")));
        }
        return out;
    }

    public String questStatus(int studentId,int questId) throws SQLException {
        if (!AppContext.usingDatabase) return memoryStudentQuests.getOrDefault(studentId+":"+questId,"NOT_STARTED");
        try(Connection c=DBConnection.get(); PreparedStatement p=c.prepareStatement("SELECT status FROM student_quests WHERE student_id=? AND quest_id=?")) {
            p.setInt(1,studentId); p.setInt(2,questId); try(ResultSet r=p.executeQuery()) { return r.next()?r.getString(1):"NOT_STARTED"; }
        }
    }

    public void advanceQuest(int studentId,int questId) throws SQLException {
        String status=questStatus(studentId,questId);
        String next=status.equals("NOT_STARTED")?"IN_PROGRESS":"COMPLETED";
        if(status.equals("COMPLETED")) return;
        if(!AppContext.usingDatabase) { memoryStudentQuests.put(studentId+":"+questId,next); return; }
        String sql="INSERT INTO student_quests(student_id,quest_id,status,completed_at) VALUES(?,?,?,CASE WHEN ?='COMPLETED' THEN CURRENT_TIMESTAMP ELSE NULL END) ON DUPLICATE KEY UPDATE status=VALUES(status), completed_at=VALUES(completed_at)";
        try(Connection c=DBConnection.get(); PreparedStatement p=c.prepareStatement(sql)) {
            p.setInt(1,studentId); p.setInt(2,questId); p.setString(3,next); p.setString(4,next); p.executeUpdate();
        }
    }

    public int completedQuestCount(int studentId) throws SQLException {
        if (!AppContext.usingDatabase) { int n=0; for(Quest q:memoryQuests) if("COMPLETED".equals(memoryStudentQuests.get(studentId+":"+q.id))) n++; return n; }
        try(Connection c=DBConnection.get(); PreparedStatement p=c.prepareStatement("SELECT COUNT(*) FROM student_quests WHERE student_id=? AND status='COMPLETED'")) {
            p.setInt(1,studentId); try(ResultSet r=p.executeQuery()) { r.next(); return r.getInt(1); }
        }
    }

    public static class MentorRequest {
        public final int id,studentId,alumniId; public final String message,createdAt; public String status;
        public MentorRequest(int id,int studentId,int alumniId,String message,String status,String createdAt) { this.id=id;this.studentId=studentId;this.alumniId=alumniId;this.message=message;this.status=status;this.createdAt=createdAt; }
    }
    public static class Roadmap {
        public final int id,studentId; public final String title,targetRole;
        public Roadmap(int id,int studentId,String title,String targetRole) { this.id=id;this.studentId=studentId;this.title=title;this.targetRole=targetRole; }
        @Override public String toString() { return title+" — "+targetRole; }
    }
    public static class Milestone {
        public final int id,roadmapId; public final String title,description; public boolean completed;
        public Milestone(int id,int roadmapId,String title,String description,boolean completed) { this.id=id;this.roadmapId=roadmapId;this.title=title;this.description=description;this.completed=completed; }
        @Override public String toString() { return (completed?"[DONE] ":"[TODO] ")+title+(description==null||description.trim().isEmpty()?"":" — "+description); }
    }
    public static class Quest {
        public final int id,points; public final String title,description;
        public Quest(int id,String title,String description,int points) { this.id=id;this.title=title;this.description=description;this.points=points; }
        @Override public String toString() { return title+" ("+points+" pts) — "+description; }
    }
}
