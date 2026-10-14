import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Member 3: data access for jobs and for each student's Save / Skip choices.
 * Uses MySQL when AppContext.usingDatabase is true and memory otherwise (same pattern as Member 2).
 */
public class JobRepository {
    private static final String COLS =
        "id,title,company,job_type,location,required_skills,description,posted_by,CAST(created_at AS CHAR) created_at";
    private static final List<Job> memoryJobs = new ArrayList<>();
    private static final Map<Integer, Map<Integer, String>> memoryActions = new HashMap<>();
    private static int nextJobId = 1;
    private static boolean ready = false;

    // title, company, type, location, required skills, description (companies match the sample alumni)
    private static final String[][] SEED = {
        {"Software Engineer Intern", "Google", "INTERNSHIP", "Bengaluru", "Java,SQL,Git,Data Structures,Cloud",
         "Work with a product team on backend services. Strong Java and SQL fundamentals expected."},
        {"Data Science Intern", "Microsoft", "INTERNSHIP", "Hyderabad", "Python,Machine Learning,SQL,Data Analysis",
         "Build and evaluate simple ML models on real product data."},
        {"Java Developer", "Infosys", "JOB", "Pune", "Java,SQL,Git,Spring Boot,AWS",
         "Develop and maintain enterprise Java applications for global clients."},
        {"Quantitative Analyst Intern", "Goldman Sachs", "INTERNSHIP", "Bengaluru", "Python,Finance,SQL,Data Analysis",
         "Support the quant team with data analysis and Python tooling."},
        {"Product Design Intern", "Figma", "INTERNSHIP", "Remote", "UI/UX,Communication",
         "Design and test user flows with a small product team."},
        {"Security Analyst", "Deloitte", "JOB", "Mumbai", "Cybersecurity,Cloud,Communication,SQL",
         "Monitor alerts, investigate incidents and write clear security reports."},
        {"Cloud Engineer", "Amazon", "JOB", "Chennai", "Cloud,Java,Cybersecurity,Linux",
         "Design and operate secure, scalable cloud infrastructure."},
        {"Backend Developer Intern", "Zerodha", "INTERNSHIP", "Bengaluru", "Java,SQL,Git,Python",
         "Build internal tools and APIs for a fast-moving fintech team."}
    };

    public JobRepository() { prepare(); }

    /** Creates the Member 3 tables (MySQL) and seeds sample jobs once. Safe to call many times. */
    public static synchronized void prepare() {
        if (ready) return;
        if (AppContext.usingDatabase) {
            try (Connection c = DBConnection.get(); Statement s = c.createStatement()) {
                for (String sql : tableSql()) s.execute(sql);
                seedDatabase(c);
            } catch (SQLException ex) {
                throw new IllegalStateException("Could not prepare opportunity tables: " + ex.getMessage(), ex);
            }
        } else {
            String now = now();
            for (String[] j : SEED)
                memoryJobs.add(new Job(nextJobId++, j[0], j[1], j[2], j[3], j[4], j[5], 0, now));
        }
        ready = true;
    }

    private static String now() { return LocalDateTime.now().withNano(0).toString().replace('T', ' '); }

    private static List<String> tableSql() {
        List<String> sql = new ArrayList<>();
        sql.add("CREATE TABLE IF NOT EXISTS jobs (id INT PRIMARY KEY AUTO_INCREMENT, title VARCHAR(150) NOT NULL, company VARCHAR(100) NOT NULL, job_type ENUM('JOB','INTERNSHIP') NOT NULL DEFAULT 'JOB', location VARCHAR(100), required_skills VARCHAR(255), description TEXT, posted_by INT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (posted_by) REFERENCES users(id) ON DELETE SET NULL)");
        sql.add("CREATE TABLE IF NOT EXISTS applications (id INT PRIMARY KEY AUTO_INCREMENT, student_id INT NOT NULL, job_id INT NOT NULL, status ENUM('APPLIED','SHORTLISTED','INTERVIEW','REJECTED') NOT NULL DEFAULT 'APPLIED', applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, notes VARCHAR(300), UNIQUE KEY uq_application (student_id, job_id), FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE, FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE)");
        sql.add("CREATE TABLE IF NOT EXISTS referrals (id INT PRIMARY KEY AUTO_INCREMENT, student_id INT NOT NULL, alumni_id INT NOT NULL, job_id INT NOT NULL, message VARCHAR(500) NOT NULL, status ENUM('PENDING','ACCEPTED','REJECTED') NOT NULL DEFAULT 'PENDING', created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE, FOREIGN KEY (alumni_id) REFERENCES users(id) ON DELETE CASCADE, FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE)");
        sql.add("CREATE TABLE IF NOT EXISTS job_actions (student_id INT NOT NULL, job_id INT NOT NULL, action ENUM('SAVED','SKIPPED') NOT NULL, PRIMARY KEY (student_id, job_id), FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE, FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE)");
        return sql;
    }

    private static void seedDatabase(Connection c) throws SQLException {
        try (PreparedStatement check = c.prepareStatement("SELECT COUNT(*) FROM jobs"); ResultSet rs = check.executeQuery()) {
            rs.next(); if (rs.getInt(1) > 0) return;
        }
        try (PreparedStatement p = c.prepareStatement(
                "INSERT INTO jobs(title,company,job_type,location,required_skills,description) VALUES(?,?,?,?,?,?)")) {
            for (String[] j : SEED) { for (int i = 0; i < 6; i++) p.setString(i + 1, j[i]); p.addBatch(); }
            p.executeBatch();
        }
    }

    // ---------------- jobs: read ----------------

    public List<Job> findAll() throws SQLException {
        if (!AppContext.usingDatabase) return new ArrayList<>(memoryJobs);
        return query("SELECT " + COLS + " FROM jobs ORDER BY id", Collections.<Object>emptyList());
    }

    public Job findById(int jobId) throws SQLException {
        if (!AppContext.usingDatabase) {
            for (Job j : memoryJobs) if (j.getId() == jobId) return j;
            return null;
        }
        List<Job> l = query("SELECT " + COLS + " FROM jobs WHERE id=?", Arrays.<Object>asList(jobId));
        return l.isEmpty() ? null : l.get(0);
    }

    /** keyword matches title, company, location or required skills; type is ALL, JOB or INTERNSHIP. */
    public List<Job> search(String keyword, String type) throws SQLException {
        String kw = keyword == null ? "" : keyword.trim();
        boolean filterType = Job.JOB.equals(type) || Job.INTERNSHIP.equals(type);
        if (!AppContext.usingDatabase) {
            List<Job> out = new ArrayList<>(); String k = kw.toLowerCase(Locale.ROOT);
            for (Job j : memoryJobs) {
                if (filterType && !j.getJobType().equals(type)) continue;
                String hay = (j.getTitle() + " " + j.getCompany() + " " + j.getLocation() + " " + j.getRequiredSkillsCsv()).toLowerCase(Locale.ROOT);
                if (k.isEmpty() || hay.contains(k)) out.add(j);
            }
            return out;
        }
        StringBuilder sql = new StringBuilder("SELECT " + COLS + " FROM jobs WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (!kw.isEmpty()) {
            sql.append(" AND (title LIKE ? OR company LIKE ? OR location LIKE ? OR required_skills LIKE ?)");
            for (int i = 0; i < 4; i++) params.add("%" + kw + "%");
        }
        if (filterType) { sql.append(" AND job_type=?"); params.add(type); }
        sql.append(" ORDER BY id");
        return query(sql.toString(), params);
    }

    private List<Job> query(String sql, List<Object> params) throws SQLException {
        List<Job> out = new ArrayList<>();
        try (Connection c = DBConnection.get(); PreparedStatement p = c.prepareStatement(sql)) {
            for (int i = 0; i < params.size(); i++) p.setObject(i + 1, params.get(i));
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) out.add(new Job(r.getInt("id"), r.getString("title"), r.getString("company"),
                    r.getString("job_type"), r.getString("location"), r.getString("required_skills"),
                    r.getString("description"), r.getInt("posted_by"), r.getString("created_at")));
            }
        }
        return out;
    }

    // ---------------- jobs: create ----------------

    /** Alumni post a new opportunity. Returns the new job id. */
    public int add(Job j) throws SQLException {
        if (j.getTitle().trim().isEmpty() || j.getCompany().trim().isEmpty())
            throw new IllegalArgumentException("Title and company are required.");
        if (j.getRequiredSkills().isEmpty())
            throw new IllegalArgumentException("Enter at least one required skill (comma separated).");
        if (j.getTitle().length() > 150 || j.getCompany().length() > 100 || j.getLocation().length() > 100
                || j.getRequiredSkillsCsv().length() > 255)
            throw new IllegalArgumentException("One of the fields is too long (title 150, company 100, location 100, skills 255).");
        if (!AppContext.usingDatabase) {
            Job saved = new Job(nextJobId++, j.getTitle().trim(), j.getCompany().trim(), j.getJobType(),
                j.getLocation().trim(), j.getRequiredSkillsCsv().trim(), j.getDescription().trim(), j.getPostedBy(), now());
            memoryJobs.add(saved);
            return saved.getId();
        }
        String sql = "INSERT INTO jobs(title,company,job_type,location,required_skills,description,posted_by) VALUES(?,?,?,?,?,?,?)";
        try (Connection c = DBConnection.get(); PreparedStatement p = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            p.setString(1, j.getTitle().trim()); p.setString(2, j.getCompany().trim()); p.setString(3, j.getJobType());
            p.setString(4, j.getLocation().trim()); p.setString(5, j.getRequiredSkillsCsv().trim());
            p.setString(6, j.getDescription().trim());
            if (j.getPostedBy() > 0) p.setInt(7, j.getPostedBy()); else p.setNull(7, Types.INTEGER);
            p.executeUpdate();
            try (ResultSet keys = p.getGeneratedKeys()) { if (keys.next()) return keys.getInt(1); }
        }
        throw new SQLException("Could not create the job.");
    }

    // ---------------- Save / Skip ----------------

    public void setAction(int studentId, int jobId, String action) throws SQLException {
        if (!"SAVED".equals(action) && !"SKIPPED".equals(action)) throw new IllegalArgumentException("Invalid action.");
        if (findById(jobId) == null) throw new IllegalArgumentException("Job not found.");
        if (!AppContext.usingDatabase) {
            Map<Integer, String> m = memoryActions.get(studentId);
            if (m == null) { m = new HashMap<>(); memoryActions.put(studentId, m); }
            m.put(jobId, action);
            return;
        }
        String sql = "INSERT INTO job_actions(student_id,job_id,action) VALUES(?,?,?) ON DUPLICATE KEY UPDATE action=VALUES(action)";
        try (Connection c = DBConnection.get(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, studentId); p.setInt(2, jobId); p.setString(3, action); p.executeUpdate();
        }
    }

    public void clearAction(int studentId, int jobId) throws SQLException {
        if (!AppContext.usingDatabase) {
            Map<Integer, String> m = memoryActions.get(studentId);
            if (m != null) m.remove(jobId);
            return;
        }
        try (Connection c = DBConnection.get(); PreparedStatement p = c.prepareStatement("DELETE FROM job_actions WHERE student_id=? AND job_id=?")) {
            p.setInt(1, studentId); p.setInt(2, jobId); p.executeUpdate();
        }
    }

    /** Puts every skipped job back into the swipe deck. */
    public void clearSkipped(int studentId) throws SQLException {
        if (!AppContext.usingDatabase) {
            Map<Integer, String> m = memoryActions.get(studentId);
            if (m != null) m.values().removeAll(Collections.singleton("SKIPPED"));
            return;
        }
        try (Connection c = DBConnection.get(); PreparedStatement p = c.prepareStatement("DELETE FROM job_actions WHERE student_id=? AND action='SKIPPED'")) {
            p.setInt(1, studentId); p.executeUpdate();
        }
    }

    /** jobId -> SAVED or SKIPPED for this student. */
    public Map<Integer, String> actionsFor(int studentId) throws SQLException {
        Map<Integer, String> out = new HashMap<>();
        if (!AppContext.usingDatabase) {
            Map<Integer, String> m = memoryActions.get(studentId);
            if (m != null) out.putAll(m);
            return out;
        }
        try (Connection c = DBConnection.get(); PreparedStatement p = c.prepareStatement("SELECT job_id,action FROM job_actions WHERE student_id=?")) {
            p.setInt(1, studentId);
            try (ResultSet r = p.executeQuery()) { while (r.next()) out.put(r.getInt(1), r.getString(2)); }
        }
        return out;
    }

    public List<Job> jobsWithAction(int studentId, String action) throws SQLException {
        Map<Integer, String> actions = actionsFor(studentId);
        List<Job> out = new ArrayList<>();
        for (Job j : findAll()) if (action.equals(actions.get(j.getId()))) out.add(j);
        return out;
    }

    /** The swipe deck: jobs this student has neither saved nor skipped. */
    public List<Job> unseenJobs(int studentId) throws SQLException {
        Map<Integer, String> actions = actionsFor(studentId);
        List<Job> out = new ArrayList<>();
        for (Job j : findAll()) if (!actions.containsKey(j.getId())) out.add(j);
        return out;
    }
}
