import java.sql.*;
import java.util.*;

/** Member 5: JDBC for leaderboard (reads M2/M3/M4 tables via v_impact), badges, achievements and analytics. */
public class ImpactRepository {

    public List<Badge> badges() throws SQLException {
        List<Badge> out = new ArrayList<>();
        try (Connection c = DBConnection.get(); Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT id, name, min_score FROM badges ORDER BY min_score")) {
            while (rs.next()) out.add(new Badge(rs.getInt(1), rs.getString(2), rs.getInt(3)));
        }
        return out;
    }

    public void updateMinScore(int badgeId, int minScore) throws SQLException {
        if (minScore < 0) throw new IllegalArgumentException("Minimum score cannot be negative.");
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement("UPDATE badges SET min_score=? WHERE id=?")) {
            ps.setInt(1, minScore); ps.setInt(2, badgeId); ps.executeUpdate();
        }
    }

    public List<CommunityImpact> leaderboard() throws SQLException {
        List<CommunityImpact> out = new ArrayList<>();
        String sql = "SELECT user_id, name, role, mentorships, helpful_answers, referrals, events "
                   + "FROM v_impact ORDER BY total_score DESC, name";
        try (Connection c = DBConnection.get(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) out.add(new CommunityImpact(rs.getInt(1), rs.getString(2), rs.getString(3),
                    rs.getInt(4), rs.getInt(5), rs.getInt(6), rs.getInt(7)));
        }
        return out;
    }

    /** Gives every user every badge they qualify for. Returns how many NEW achievements were created. */
    public int awardBadges() throws SQLException {
        List<Badge> badges = badges();
        List<CommunityImpact> board = leaderboard();
        int created = 0;
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement("INSERT IGNORE INTO achievements(user_id, badge_id) VALUES(?,?)")) {
            for (CommunityImpact ci : board)
                for (Badge b : badges)
                    if (ci.getTotal() >= b.getMinScore()) {
                        ps.setInt(1, ci.getUserId()); ps.setInt(2, b.getId());
                        created += ps.executeUpdate();
                    }
        }
        return created;
    }

    public List<Achievement> achievements() throws SQLException {
        List<Achievement> out = new ArrayList<>();
        String sql = "SELECT a.user_id, u.full_name, b.name, a.awarded_at FROM achievements a "
                   + "JOIN users u ON u.id=a.user_id JOIN badges b ON b.id=a.badge_id ORDER BY a.awarded_at DESC, b.min_score DESC";
        try (Connection c = DBConnection.get(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) out.add(new Achievement(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4)));
        }
        return out;
    }

    // ---------- Analytics ----------
    public Map<String, String> summary() throws SQLException {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("Total users", one("SELECT COUNT(*) FROM users"));
        m.put("Students", one("SELECT COUNT(*) FROM users WHERE user_role='STUDENT'"));
        m.put("Alumni", one("SELECT COUNT(*) FROM users WHERE user_role='ALUMNI'"));
        m.put("Mentorship requests (M2)", one("SELECT COUNT(*) FROM mentorship_requests"));
        m.put("Mentorships accepted (M2)", one("SELECT COUNT(*) FROM mentorship_requests WHERE status='ACCEPTED'"));
        m.put("Job postings (M3)", one("SELECT COUNT(*) FROM jobs"));
        m.put("Job applications (M3)", one("SELECT COUNT(*) FROM applications"));
        m.put("Referral requests (M3)", one("SELECT COUNT(*) FROM referrals"));
        m.put("Referrals accepted (M3)", one("SELECT COUNT(*) FROM referrals WHERE status='ACCEPTED'"));
        m.put("Questions asked (M4)", one("SELECT COUNT(*) FROM qa_questions"));
        m.put("Helpful answers (M4)", one("SELECT COUNT(*) FROM qa_answers WHERE is_helpful=1"));
        m.put("Discussion posts (M4)", one("SELECT COUNT(*) FROM discussion_posts"));
        m.put("Event RSVPs (M4)", one("SELECT COUNT(*) FROM event_rsvps"));
        m.put("Reports pending", one("SELECT COUNT(*) FROM reports WHERE status='PENDING'"));
        m.put("Reports closed", one("SELECT COUNT(*) FROM reports WHERE status<>'PENDING'"));
        m.put("Blocked users", one("SELECT COUNT(*) FROM blocked_users"));
        m.put("Feedback received", one("SELECT COUNT(*) FROM feedback"));
        m.put("Average rating", one("SELECT COALESCE(ROUND(AVG(rating),2),0) FROM feedback"));
        m.put("Badges awarded", one("SELECT COUNT(*) FROM achievements"));
        return m;
    }

    /** rows: {month, activity kind, count} across Members 2, 3 and 4. */
    public List<Object[]> monthlyActivity() throws SQLException {
        List<Object[]> out = new ArrayList<>();
        String sql = "SELECT m, kind, COUNT(*) FROM ("
            + " SELECT DATE_FORMAT(created_at,'%Y-%m') m, 'Mentorship requests' kind FROM mentorship_requests"
            + " UNION ALL SELECT DATE_FORMAT(created_at,'%Y-%m'), 'Referral requests' FROM referrals"
            + " UNION ALL SELECT DATE_FORMAT(created_at,'%Y-%m'), 'Discussion posts' FROM discussion_posts"
            + " UNION ALL SELECT DATE_FORMAT(created_at,'%Y-%m'), 'Questions asked' FROM qa_questions) x"
            + " GROUP BY m, kind ORDER BY m, kind";
        try (Connection c = DBConnection.get(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) out.add(new Object[]{rs.getString(1), rs.getString(2), rs.getInt(3)});
        }
        return out;
    }

    private String one(String sql) throws SQLException {
        try (Connection c = DBConnection.get(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            rs.next(); return rs.getString(1);
        }
    }
}
