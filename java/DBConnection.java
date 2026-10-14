import java.sql.*;

public class DBConnection {
    private static final String URL  = "jdbc:mysql://localhost:3306/alumni_db";
    private static final String USER = "root";
    private static final String PASS = "";        // <-- your MySQL password

    /** Shared credentials so every module (incl. Member 4's own pool) uses ONE MySQL password. */
    public static String url()  { return URL; }
    public static String user() { return USER; }
    public static String pass() { return PASS; }

    public static Connection get() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }

    /** True only if MySQL is reachable AND schema.sql has been run. */
    public static boolean isAvailable() {
        DriverManager.setLoginTimeout(3);
        try (Connection c = get(); Statement s = c.createStatement()) {
            s.executeQuery("SELECT 1 FROM users LIMIT 1").close();
            return true;
        } catch (Exception e) {
            System.out.println("MySQL not available (" + e.getMessage() + ") - using in-memory data.");
            return false;
        }
    }
}
