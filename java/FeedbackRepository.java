import java.sql.*;
import java.util.*;

/** Member 5: JDBC for user feedback. */
public class FeedbackRepository {

    public List<Feedback> all() throws SQLException {
        List<Feedback> out = new ArrayList<>();
        String sql = "SELECT f.id, f.user_id, u.full_name, f.message, f.rating, f.status, f.created_at "
                   + "FROM feedback f JOIN users u ON u.id=f.user_id ORDER BY f.created_at DESC, f.id DESC";
        try (Connection c = DBConnection.get(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) out.add(new Feedback(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getString(4),
                    rs.getInt(5), rs.getString(6), rs.getString(7)));
        }
        return out;
    }

    public void create(int userId, String message, int rating) throws SQLException {
        Validator.text(message, "Message", 1000);
        if (rating < 1 || rating > 5) throw new IllegalArgumentException("Rating must be between 1 and 5.");
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement("INSERT INTO feedback(user_id, message, rating) VALUES(?,?,?)")) {
            ps.setInt(1, userId); ps.setString(2, message.trim()); ps.setInt(3, rating);
            ps.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException e) {
            throw new IllegalArgumentException("No user with id " + userId + ".");
        }
    }

    public void updateStatus(int id, String status) throws SQLException {
        if (!Arrays.asList(Feedback.NEW, Feedback.REVIEWED, Feedback.ACTIONED).contains(status))
            throw new IllegalArgumentException("Invalid feedback status.");
        try (Connection c = DBConnection.get(); PreparedStatement ps = c.prepareStatement("UPDATE feedback SET status=? WHERE id=?")) {
            ps.setString(1, status); ps.setInt(2, id); ps.executeUpdate();
        }
    }
}
