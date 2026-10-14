import java.sql.*;
import java.util.*;

/** Member 5: JDBC for moderation (reports + blocking). */
public class ReportRepository {

    public List<Report> find(String status) throws SQLException {   // status null/"ALL" = everything
        List<Report> out = new ArrayList<>();
        boolean all = status == null || status.equals("ALL");
        String sql = "SELECT r.id, r.reporter_id, u1.full_name, r.reported_id, u2.full_name, r.target_type, r.target_id, r.reason, r.status, r.admin_note, r.created_at "
                   + "FROM reports r JOIN users u1 ON u1.id=r.reporter_id JOIN users u2 ON u2.id=r.reported_id "
                   + (all ? "" : "WHERE r.status=? ") + "ORDER BY r.created_at DESC, r.id DESC";
        try (Connection c = DBConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (!all) ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(new Report(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getInt(4),
                        rs.getString(5), rs.getString(6), rs.getInt(7), rs.getString(8), rs.getString(9), rs.getString(10), rs.getString(11)));
            }
        }
        return out;
    }

    /** Report a user. */
    public int create(int reporterId, int reportedId, String reason) throws SQLException {
        return create(reporterId, reportedId, Report.USER, reportedId, reason);
    }

    /** Report a user or Member 4 content. reportedId = the author (a users id). */
    public int create(int reporterId, int reportedId, String targetType, int targetId, String reason) throws SQLException {
        if (reporterId == reportedId) throw new IllegalArgumentException("A user cannot report themselves.");
        Validator.text(reason, "Reason", 500);
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement("INSERT INTO reports(reporter_id, reported_id, target_type, target_id, reason) VALUES(?,?,?,?,?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, reporterId); ps.setInt(2, reportedId); ps.setString(3, targetType); ps.setInt(4, targetId); ps.setString(5, reason.trim());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { k.next(); return k.getInt(1); }
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new IllegalArgumentException("Reporter or reported user id does not exist.");
        }
    }

    /** Finds the author (user id) of a Member 4 answer / post. Throws if the content does not exist. */
    public int authorOf(String targetType, int targetId) throws SQLException {
        String sql;
        if (Report.QA_ANSWER.equals(targetType)) sql = "SELECT alumni_id FROM qa_answers WHERE id=?";
        else if (Report.DISCUSSION_POST.equals(targetType)) sql = "SELECT author_id FROM discussion_posts WHERE id=?";
        else throw new IllegalArgumentException("Unknown content type: " + targetType);
        try (Connection c = DBConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, targetId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException(targetType + " #" + targetId + " does not exist.");
                return rs.getInt(1);
            }
        }
    }

    /** Deletes the reported Member 4 content and resolves the report (one transaction). */
    public void deleteContentAndResolve(Report r, int adminId) throws SQLException {
        if (!r.isContent()) throw new IllegalArgumentException("This report is about a user, not content.");
        String table = Report.QA_ANSWER.equals(r.getTargetType()) ? "qa_answers" : "discussion_posts";
        try (Connection c = DBConnection.get()) {
            c.setAutoCommit(false);
            try (PreparedStatement d = c.prepareStatement("DELETE FROM " + table + " WHERE id=?");
                 PreparedStatement u = c.prepareStatement("UPDATE reports SET status='RESOLVED', admin_id=?, admin_note='Content removed', resolved_at=NOW() WHERE id=? AND status='PENDING'")) {
                d.setInt(1, r.getTargetId()); d.executeUpdate();
                if (adminId > 0) u.setInt(1, adminId); else u.setNull(1, Types.INTEGER);
                u.setInt(2, r.getId()); u.executeUpdate();
                c.commit();
            } catch (SQLException e) { c.rollback(); throw e; }
        }
    }

    /** Closes a PENDING report as RESOLVED or DISMISSED. Returns false if it was not pending. */
    public boolean close(int reportId, int adminId, String newStatus, String note) throws SQLException {
        if (!Report.RESOLVED.equals(newStatus) && !Report.DISMISSED.equals(newStatus))
            throw new IllegalArgumentException("Status must be RESOLVED or DISMISSED.");
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement("UPDATE reports SET status=?, admin_id=?, admin_note=?, resolved_at=NOW() "
                     + "WHERE id=? AND status='PENDING'")) {
            ps.setString(1, newStatus);
            if (adminId > 0) ps.setInt(2, adminId); else ps.setNull(2, Types.INTEGER);
            ps.setString(3, note);
            ps.setInt(4, reportId);
            return ps.executeUpdate() == 1;
        }
    }

    /** Blocks the reported user and resolves the report in ONE transaction. */
    public void blockAndResolve(Report r, int adminId) throws SQLException {
        try (Connection c = DBConnection.get()) {
            c.setAutoCommit(false);
            try (PreparedStatement b = c.prepareStatement("INSERT IGNORE INTO blocked_users(user_id, reason) VALUES(?,?)");
                 PreparedStatement u = c.prepareStatement("UPDATE reports SET status='RESOLVED', admin_id=?, admin_note='User blocked', resolved_at=NOW() "
                         + "WHERE id=? AND status='PENDING'")) {
                b.setInt(1, r.getReportedId()); b.setString(2, "Report #" + r.getId() + ": " + r.getReason());
                b.executeUpdate();
                if (adminId > 0) u.setInt(1, adminId); else u.setNull(1, Types.INTEGER);
                u.setInt(2, r.getId());
                u.executeUpdate();
                c.commit();
            } catch (SQLException e) { c.rollback(); throw e; }
        }
    }

    public boolean isBlocked(int userId) throws SQLException {
        try (Connection c = DBConnection.get(); PreparedStatement ps = c.prepareStatement("SELECT 1 FROM blocked_users WHERE user_id=?")) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }
}
