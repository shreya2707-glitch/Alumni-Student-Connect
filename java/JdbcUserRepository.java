import java.sql.*;
import java.util.*;

/** MySQL implementation (JDBC SELECT / INSERT / UPDATE with PreparedStatements). */
public class JdbcUserRepository implements UserRepository, AlumniRepository {

    private static final String ALUMNI_SELECT =
        "SELECT u.id,u.username,u.full_name,u.email,u.bio,a.company,a.job_role,a.industry,a.grad_year," +
        "a.skills,a.interests,a.goal,a.qualification,a.journey " +
        "FROM users u JOIN alumni_profiles a ON a.user_id=u.id";

    private static RuntimeException fail(SQLException e) { return new RuntimeException(e); }

    // ---------- AlumniRepository ----------
    public List<Alumni> findAllAlumni() { return queryAlumni(ALUMNI_SELECT + " ORDER BY u.id", null); }
    public List<Alumni> findAlumniByIndustry(String ind) { return queryAlumni(ALUMNI_SELECT + " WHERE a.industry=?", ind); }
    public Alumni findAlumniById(int id) {
        List<Alumni> l = queryAlumni(ALUMNI_SELECT + " WHERE u.id=?", id);
        return l.isEmpty() ? null : l.get(0);
    }

    private List<Alumni> queryAlumni(String sql, Object param) {
        List<Alumni> out = new ArrayList<>();
        try (Connection c = DBConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (param != null) ps.setObject(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Alumni a = new Alumni(rs.getInt("id"), rs.getString("username"), rs.getString("full_name"),
                        rs.getString("email"), rs.getString("company"), rs.getString("job_role"),
                        rs.getString("industry"), rs.getInt("grad_year"), rs.getString("skills"),
                        rs.getString("interests"), rs.getString("goal"), rs.getString("qualification"),
                        rs.getString("journey"));
                    a.setBio(rs.getString("bio"));
                    out.add(a);
                }
            }
        } catch (SQLException e) { throw fail(e); }
        return out;
    }

    // ---------- UserRepository ----------
    public User findById(int id) { return findOne("WHERE id=?", id); }
    public User findByUsername(String name) { return findOne("WHERE username=?", name); }

    private User findOne(String where, Object p) {
        String sql = "SELECT id,username,full_name,email,bio,user_role FROM users " + where;
        try (Connection c = DBConnection.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, p);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? build(c, rs) : null; }
        } catch (SQLException e) { throw fail(e); }
    }

    public List<User> findAllUsers() {
        List<User> out = new ArrayList<>();
        try (Connection c = DBConnection.get(); Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT id,username,full_name,email,bio,user_role FROM users ORDER BY id")) {
            while (rs.next()) out.add(build(c, rs));
        } catch (SQLException e) { throw fail(e); }
        return out;
    }

    /** Builds the right subclass (polymorphic load) from a users row. */
    private User build(Connection c, ResultSet u) throws SQLException {
        int id = u.getInt("id");
        String un = u.getString("username"), fn = u.getString("full_name"), em = u.getString("email");
        User user;
        switch (Role.valueOf(u.getString("user_role"))) {
            case ALUMNI:  user = loadAlumni(c, id, un, fn, em); break;
            case STUDENT: user = loadStudent(c, id, un, fn, em); break;
            default:      user = new Admin(id, un, fn, em);
        }
        user.setBio(u.getString("bio"));
        return user;
    }

    private User loadAlumni(Connection c, int id, String un, String fn, String em) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM alumni_profiles WHERE user_id=?")) {
            ps.setInt(1, id);
            try (ResultSet r = ps.executeQuery()) {
                if (!r.next()) return new Alumni(id, un, fn, em, "", "", "", 0, "", "", "", "", "");
                return new Alumni(id, un, fn, em, r.getString("company"), r.getString("job_role"),
                    r.getString("industry"), r.getInt("grad_year"), r.getString("skills"),
                    r.getString("interests"), r.getString("goal"), r.getString("qualification"),
                    r.getString("journey"));
            }
        }
    }

    private User loadStudent(Connection c, int id, String un, String fn, String em) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT * FROM student_profiles WHERE user_id=?")) {
            ps.setInt(1, id);
            try (ResultSet r = ps.executeQuery()) {
                if (!r.next()) return new Student(id, un, fn, em, "", "", "", "");
                return new Student(id, un, fn, em, r.getString("skills"), r.getString("interests"),
                    r.getString("target_industry"), r.getString("goal"));
            }
        }
    }

    public boolean usernameExists(String name) { return getPasswordHash(name) != null; }

    public String getPasswordHash(String name) {
        try (Connection c = DBConnection.get();
             PreparedStatement ps = c.prepareStatement("SELECT password_hash FROM users WHERE username=?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getString(1) : null; }
        } catch (SQLException e) { throw fail(e); }
    }

    public User save(User u, String hash) {
        try (Connection c = DBConnection.get()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO users(username,password_hash,user_role,full_name,email,bio) VALUES(?,?,?,?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, u.getUsername()); ps.setString(2, hash);
                    ps.setString(3, u.getType().name()); ps.setString(4, u.getFullName());
                    ps.setString(5, u.getEmail()); ps.setString(6, u.getBio());
                    ps.executeUpdate();
                    try (ResultSet k = ps.getGeneratedKeys()) { k.next(); u.setId(k.getInt(1)); }
                }
                writeProfile(c, u, true);
                c.commit();
            } catch (SQLException e) { c.rollback(); throw e; }
        } catch (SQLException e) { throw fail(e); }
        return u;
    }

    public void updateProfile(User u) {
        try (Connection c = DBConnection.get()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE users SET full_name=?, email=?, bio=? WHERE id=?")) {
                    ps.setString(1, u.getFullName()); ps.setString(2, u.getEmail());
                    ps.setString(3, u.getBio()); ps.setInt(4, u.getId());
                    ps.executeUpdate();
                }
                writeProfile(c, u, false);
                c.commit();
            } catch (SQLException e) { c.rollback(); throw e; }
        } catch (SQLException e) { throw fail(e); }
    }

    /** INSERT or UPDATE the type-specific profile row (user_id is always the last parameter). */
    private void writeProfile(Connection c, User u, boolean insert) throws SQLException {
        if (u instanceof Alumni) {
            Alumni a = (Alumni) u;
            String sql = insert
                ? "INSERT INTO alumni_profiles(company,job_role,industry,grad_year,skills,interests,goal,qualification,journey,user_id) VALUES(?,?,?,?,?,?,?,?,?,?)"
                : "UPDATE alumni_profiles SET company=?,job_role=?,industry=?,grad_year=?,skills=?,interests=?,goal=?,qualification=?,journey=? WHERE user_id=?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, a.getCompany()); ps.setString(2, a.getJobRole());
                ps.setString(3, a.getIndustry()); ps.setInt(4, a.getGradYear());
                ps.setString(5, a.getSkillsCsv()); ps.setString(6, a.getInterestsCsv());
                ps.setString(7, a.getGoal()); ps.setString(8, a.getQualification());
                ps.setString(9, a.getJourney()); ps.setInt(10, a.getId());
                ps.executeUpdate();
            }
        } else if (u instanceof Student) {
            Student s = (Student) u;
            String sql = insert
                ? "INSERT INTO student_profiles(skills,interests,target_industry,goal,user_id) VALUES(?,?,?,?,?)"
                : "UPDATE student_profiles SET skills=?,interests=?,target_industry=?,goal=? WHERE user_id=?";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, s.getSkillsCsv()); ps.setString(2, s.getInterestsCsv());
                ps.setString(3, s.getTargetIndustry()); ps.setString(4, s.getGoal());
                ps.setInt(5, s.getId());
                ps.executeUpdate();
            }
        }
    }
}
