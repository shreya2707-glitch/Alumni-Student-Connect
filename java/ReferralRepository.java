import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/** Member 3: alumni referral requests. MySQL when available, memory otherwise. */
public class ReferralRepository {
    private static final String COLS = "id,student_id,alumni_id,job_id,message,status,CAST(created_at AS CHAR) created_at";
    private static final List<Referral> memoryReferrals = new ArrayList<>();
    private static int nextId = 1;

    public ReferralRepository() { JobRepository.prepare(); }   // makes sure the tables exist

    private boolean activeRequestExists(int studentId, int alumniId, int jobId) throws SQLException {
        if (!AppContext.usingDatabase) {
            for (Referral r : memoryReferrals)
                if (r.getStudentId() == studentId && r.getAlumniId() == alumniId && r.getJobId() == jobId
                        && !Referral.REJECTED.equals(r.getStatus())) return true;
            return false;
        }
        String sql = "SELECT COUNT(*) FROM referrals WHERE student_id=? AND alumni_id=? AND job_id=? AND status IN ('PENDING','ACCEPTED')";
        try (Connection c = DBConnection.get(); PreparedStatement p = c.prepareStatement(sql)) {
            p.setInt(1, studentId); p.setInt(2, alumniId); p.setInt(3, jobId);
            try (ResultSet r = p.executeQuery()) { r.next(); return r.getInt(1) > 0; }
        }
    }

    public void create(int studentId, int alumniId, int jobId, String message) throws SQLException {
        if (studentId == alumniId) throw new IllegalArgumentException("You cannot request a referral from yourself.");
        String msg = message == null ? "" : message.trim();
        if (msg.isEmpty()) throw new IllegalArgumentException("Please write a short referral message.");
        if (msg.length() > 500) throw new IllegalArgumentException("The message can be at most 500 characters.");
        if (activeRequestExists(studentId, alumniId, jobId))
            throw new IllegalArgumentException("You already have a pending or accepted request to this alumnus for this job.");
        if (!AppContext.usingDatabase) {
            String now = LocalDateTime.now().withNano(0).toString().replace('T', ' ');
            memoryReferrals.add(0, new Referral(nextId++, studentId, alumniId, jobId, msg, Referral.PENDING, now));
            return;
        }
        try (Connection c = DBConnection.get(); PreparedStatement p = c.prepareStatement("INSERT INTO referrals(student_id,alumni_id,job_id,message) VALUES(?,?,?,?)")) {
            p.setInt(1, studentId); p.setInt(2, alumniId); p.setInt(3, jobId); p.setString(4, msg); p.executeUpdate();
        }
    }

    public List<Referral> forStudent(int studentId) throws SQLException { return load("student_id", studentId); }

    public List<Referral> forAlumni(int alumniId) throws SQLException { return load("alumni_id", alumniId); }

    private List<Referral> load(String column, int userId) throws SQLException {
        List<Referral> out = new ArrayList<>();
        if (!AppContext.usingDatabase) {
            for (Referral r : memoryReferrals)
                if ((column.equals("student_id") ? r.getStudentId() : r.getAlumniId()) == userId) out.add(r);
            return out;
        }
        try (Connection c = DBConnection.get(); PreparedStatement p = c.prepareStatement("SELECT " + COLS + " FROM referrals WHERE " + column + "=? ORDER BY id DESC")) {
            p.setInt(1, userId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) out.add(new Referral(r.getInt("id"), r.getInt("student_id"), r.getInt("alumni_id"),
                    r.getInt("job_id"), r.getString("message"), r.getString("status"), r.getString("created_at")));
            }
        }
        return out;
    }

    /** Only the alumnus the request was sent to can answer it, and only while it is PENDING. */
    public void updateStatus(int referralId, int alumniId, String status) throws SQLException {
        if (!Referral.ACCEPTED.equals(status) && !Referral.REJECTED.equals(status))
            throw new IllegalArgumentException("Invalid referral status.");
        if (!AppContext.usingDatabase) {
            for (Referral r : memoryReferrals)
                if (r.getId() == referralId && r.getAlumniId() == alumniId && Referral.PENDING.equals(r.getStatus())) { r.setStatus(status); return; }
            throw new IllegalArgumentException("Pending request not found.");
        }
        try (Connection c = DBConnection.get(); PreparedStatement p = c.prepareStatement("UPDATE referrals SET status=? WHERE id=? AND alumni_id=? AND status='PENDING'")) {
            p.setString(1, status); p.setInt(2, referralId); p.setInt(3, alumniId);
            if (p.executeUpdate() == 0) throw new IllegalArgumentException("Pending request not found.");
        }
    }
}
