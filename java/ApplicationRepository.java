import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/** Member 3: application CRUD for the Application Tracker. MySQL when available, memory otherwise. */
public class ApplicationRepository {
    private static final List<Application> memoryApps = new ArrayList<>();
    private static int nextId = 1;
    private final JobRepository jobs = new JobRepository();   // also makes sure the tables exist

    public boolean hasApplied(int studentId, int jobId) throws SQLException {
        if (!AppContext.usingDatabase) {
            for (Application a : memoryApps) if (a.getStudentId() == studentId && a.getJobId() == jobId) return true;
            return false;
        }
        try (Connection c = DBConnection.get(); PreparedStatement p = c.prepareStatement("SELECT COUNT(*) FROM applications WHERE student_id=? AND job_id=?")) {
            p.setInt(1, studentId); p.setInt(2, jobId);
            try (ResultSet r = p.executeQuery()) { r.next(); return r.getInt(1) > 0; }
        }
    }

    public void apply(int studentId, int jobId) throws SQLException {
        Job job = jobs.findById(jobId);
        if (job == null) throw new IllegalArgumentException("That opportunity no longer exists.");
        if (hasApplied(studentId, jobId)) throw new IllegalArgumentException("You have already applied to this opportunity.");
        if (!AppContext.usingDatabase) {
            String now = LocalDateTime.now().withNano(0).toString().replace('T', ' ');
            memoryApps.add(0, new Application(nextId++, studentId, jobId, Application.APPLIED, now, "", job.getTitle(), job.getCompany()));
            return;
        }
        try (Connection c = DBConnection.get(); PreparedStatement p = c.prepareStatement("INSERT INTO applications(student_id,job_id) VALUES(?,?)")) {
            p.setInt(1, studentId); p.setInt(2, jobId); p.executeUpdate();
        }
    }

    public List<Application> forStudent(int studentId) throws SQLException {
        List<Application> out = new ArrayList<>();
        if (!AppContext.usingDatabase) {
            for (Application a : memoryApps) if (a.getStudentId() == studentId) out.add(a);
            return out;
        }
        String sql = "SELECT a.id,a.student_id,a.job_id,a.status,CAST(a.applied_at AS CHAR) applied_at,a.notes,j.title,j.company "
                   + "FROM applications a JOIN jobs j ON j.id=a.job_id WHERE a.student_id=? ORDER BY a.id DESC";
        try (Connection c = DBConnection.get(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, studentId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) out.add(new Application(r.getInt("id"), r.getInt("student_id"), r.getInt("job_id"),
                    r.getString("status"), r.getString("applied_at"), r.getString("notes"), r.getString("title"), r.getString("company")));
            }
        }
        return out;
    }

    public void updateStatus(int applicationId, int studentId, String status, String notes) throws SQLException {
        if (!Application.isValidStatus(status)) throw new IllegalArgumentException("Invalid application status.");
        String note = notes == null ? "" : notes.trim();
        if (note.length() > 300) throw new IllegalArgumentException("Notes can be at most 300 characters.");
        if (!AppContext.usingDatabase) {
            for (Application a : memoryApps)
                if (a.getId() == applicationId && a.getStudentId() == studentId) { a.setStatus(status); a.setNotes(note); return; }
            throw new IllegalArgumentException("Application not found.");
        }
        try (Connection c = DBConnection.get(); PreparedStatement p = c.prepareStatement("UPDATE applications SET status=?, notes=? WHERE id=? AND student_id=?")) {
            p.setString(1, status); p.setString(2, note); p.setInt(3, applicationId); p.setInt(4, studentId);
            if (p.executeUpdate() == 0) throw new IllegalArgumentException("Application not found.");
        }
    }

    public void delete(int applicationId, int studentId) throws SQLException {
        if (!AppContext.usingDatabase) {
            Iterator<Application> it = memoryApps.iterator();
            while (it.hasNext()) {
                Application a = it.next();
                if (a.getId() == applicationId && a.getStudentId() == studentId) { it.remove(); return; }
            }
            throw new IllegalArgumentException("Application not found.");
        }
        try (Connection c = DBConnection.get(); PreparedStatement p = c.prepareStatement("DELETE FROM applications WHERE id=? AND student_id=?")) {
            p.setInt(1, applicationId); p.setInt(2, studentId);
            if (p.executeUpdate() == 0) throw new IllegalArgumentException("Application not found.");
        }
    }
}
