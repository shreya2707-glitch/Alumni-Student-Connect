import java.sql.*;
import java.util.*;

/** Member 5: plain-Java tests (no JUnit needed). Run: java -cp "out:lib/*" M5Tests */
public class M5Tests {
    static int pass = 0, fail = 0;
    static void check(String name, boolean ok) { System.out.println((ok ? "PASS  " : "FAIL  ") + name); if (ok) pass++; else fail++; }
    static boolean throwsIAE(Runnable r) { try { r.run(); return false; } catch (IllegalArgumentException e) { return true; } }

    public static void main(String[] a) throws Exception {
        List<Badge> b = Arrays.asList(new Badge(1, "Bronze", 20), new Badge(2, "Silver", 50), new Badge(3, "Gold", 100), new Badge(4, "Platinum", 200));
        // --- Impact score + badge calculation ---
        check("score formula 1,1,1,1 = 50", ImpactCalculator.score(1, 1, 1, 1) == 50);
        check("score formula 2,3,1,4 = 105", ImpactCalculator.score(2, 3, 1, 4) == 105);
        check("zero activity = 0", ImpactCalculator.score(0, 0, 0, 0) == 0);
        check("negative count rejected", throwsIAE(() -> ImpactCalculator.score(-1, 0, 0, 0)));
        check("badge 0 = None", ImpactCalculator.badgeFor(0, b).equals("None"));
        check("badge 19 = None (boundary)", ImpactCalculator.badgeFor(19, b).equals("None"));
        check("badge 20 = Bronze (boundary)", ImpactCalculator.badgeFor(20, b).equals("Bronze"));
        check("badge 99 = Silver", ImpactCalculator.badgeFor(99, b).equals("Silver"));
        check("badge 100 = Gold", ImpactCalculator.badgeFor(100, b).equals("Gold"));
        check("badge 500 = Platinum", ImpactCalculator.badgeFor(500, b).equals("Platinum"));
        // --- Invalid input ---
        check("empty text rejected", throwsIAE(() -> Validator.text("  ", "Reason", 10)));
        check("too long text rejected", throwsIAE(() -> Validator.text("abcdefghijk", "Reason", 10)));
        check("non-number id rejected", throwsIAE(() -> Validator.positiveInt("abc", "Id")));
        check("zero id rejected", throwsIAE(() -> Validator.positiveInt("0", "Id")));
        check("rating 6 rejected", throwsIAE(() -> Validator.rating("6")));
        check("rating 5 accepted", Validator.rating("5") == 5);

        // --- Database tests (real M2 / M3 / M4 tables) ---
        if (!DBConnection.isAvailable()) { System.out.println("SKIP  database tests (MySQL not available)"); done(); return; }
        ImpactRepository impact = new ImpactRepository(); ReportRepository rep = new ReportRepository(); FeedbackRepository fb = new FeedbackRepository();
        List<CommunityImpact> board = impact.leaderboard();
        boolean sorted = true;
        for (int i = 1; i < board.size(); i++) if (board.get(i - 1).getTotal() < board.get(i).getTotal()) sorted = false;
        check("leaderboard sorted by score desc", sorted);
        check("leaderboard excludes admins", board.stream().noneMatch(c -> "ADMIN".equals(c.getRole())));
        impact.awardBadges();
        check("awarding badges twice creates no duplicates", impact.awardBadges() == 0);
        check("analytics has data", impact.summary().size() >= 15 && !impact.monthlyActivity().isEmpty());

        int student = idOf("STUDENT"), alumni = idOf("ALUMNI");
        if (student > 0 && alumni > 0) {
            // score really follows the other members' tables
            int base = scoreOf(impact, alumni);
            exec("INSERT INTO mentorship_requests(student_id,alumni_id,message,status) VALUES(" + student + "," + alumni + ",'t','ACCEPTED')");
            check("M2 accepted mentorship adds +20", scoreOf(impact, alumni) == base + 20);
            exec("INSERT INTO mentorship_requests(student_id,alumni_id,message,status) VALUES(" + student + "," + alumni + ",'t','PENDING')");
            check("M2 pending mentorship adds 0", scoreOf(impact, alumni) == base + 20);
            exec("INSERT INTO qa_answers(question_id,alumni_id,answer_text,is_helpful) VALUES(1," + alumni + ",'t',1)");
            check("M4 helpful answer adds +10", scoreOf(impact, alumni) == base + 30);
            exec("INSERT INTO campus_events(title,event_date) VALUES('M5TEST','2026-01-01')");
            int ev = lastId("campus_events");
            exec("INSERT INTO event_rsvps(event_id,user_id) VALUES(" + ev + "," + alumni + ")");
            check("M4 event RSVP adds +5", scoreOf(impact, alumni) == base + 35);
            exec("INSERT INTO jobs(title,company) VALUES('M5TEST','X')");
            int job = lastId("jobs");
            exec("INSERT INTO referrals(student_id,alumni_id,job_id,message,status) VALUES(" + student + "," + alumni + "," + job + ",'t','ACCEPTED')");
            check("M3 accepted referral adds +15", scoreOf(impact, alumni) == base + 50);

            // user report lifecycle
            int id = rep.create(student, alumni, "Test report");
            check("report created as PENDING", rep.find("PENDING").stream().anyMatch(r -> r.getId() == id));
            check("report resolved", rep.close(id, 0, Report.RESOLVED, "ok"));
            check("resolved report cannot be closed again", !rep.close(id, 0, Report.DISMISSED, "again"));
            check("invalid status rejected", throwsIAE(() -> { try { rep.close(id, 0, "BANNED", ""); } catch (SQLException e) { throw new RuntimeException(e); } }));
            check("self-report rejected", throwsIAE(() -> { try { rep.create(student, student, "x"); } catch (SQLException e) { throw new RuntimeException(e); } }));
            check("report with unknown user rejected", throwsIAE(() -> { try { rep.create(student, 999999, "x"); } catch (SQLException e) { throw new RuntimeException(e); } }));

            // block
            int br = rep.create(student, alumni, "Block test");
            Report blockTarget = rep.find("PENDING").stream().filter(r -> r.getId() == br).findFirst().get();
            rep.blockAndResolve(blockTarget, 0);
            check("blocked user is recorded", rep.isBlocked(alumni));
            exec("DELETE FROM blocked_users WHERE user_id=" + alumni);

            // Member 4 content moderation
            exec("INSERT INTO discussion_posts(circle_id,author_id,title,content) VALUES(1," + alumni + ",'M5TEST','bad post')");
            int post = lastId("discussion_posts");
            check("authorOf finds the post author", rep.authorOf(Report.DISCUSSION_POST, post) == alumni);
            int cr = rep.create(student, alumni, Report.DISCUSSION_POST, post, "Inappropriate post");
            Report cReport = rep.find("PENDING").stream().filter(r -> r.getId() == cr).findFirst().get();
            check("content report knows its target", cReport.isContent() && cReport.getTargetId() == post);
            rep.deleteContentAndResolve(cReport, 0);
            check("reported M4 post deleted + report resolved", count("SELECT COUNT(*) FROM discussion_posts WHERE id=" + post) == 0
                && rep.find("RESOLVED").stream().anyMatch(r -> r.getId() == cr));
            check("content that does not exist is rejected", throwsIAE(() -> { try { rep.authorOf(Report.QA_ANSWER, 999999); } catch (SQLException e) { throw new RuntimeException(e); } }));
            check("user report cannot be 'content deleted'", throwsIAE(() -> { try { rep.deleteContentAndResolve(rep.find("RESOLVED").stream().filter(r -> r.getId() == id).findFirst().get(), 0); } catch (SQLException e) { throw new RuntimeException(e); } }));

            // feedback
            fb.create(student, "Test feedback", 4);
            check("feedback saved", fb.all().stream().anyMatch(f -> f.getMessage().equals("Test feedback")));
            check("feedback rating 9 rejected", throwsIAE(() -> { try { fb.create(student, "x", 9); } catch (SQLException e) { throw new RuntimeException(e); } }));

            // cleanup test rows
            exec("DELETE FROM referrals WHERE message='t'"); exec("DELETE FROM jobs WHERE title='M5TEST'");
            exec("DELETE FROM event_rsvps WHERE event_id=" + ev); exec("DELETE FROM campus_events WHERE title='M5TEST'");
            exec("DELETE FROM qa_answers WHERE answer_text='t'"); exec("DELETE FROM mentorship_requests WHERE message='t'");
            exec("DELETE FROM feedback WHERE message='Test feedback'");
        } else System.out.println("SKIP  integration tests (need at least one STUDENT and one ALUMNI user)");
        done();
    }

    static int scoreOf(ImpactRepository r, int userId) throws SQLException {
        for (CommunityImpact c : r.leaderboard()) if (c.getUserId() == userId) return c.getTotal();
        return -1;
    }
    static int idOf(String role) throws SQLException { return count("SELECT MIN(id) FROM users WHERE user_role='" + role + "'"); }
    static int lastId(String table) throws SQLException { return count("SELECT MAX(id) FROM " + table); }
    static int count(String sql) throws SQLException {
        try (Connection c = DBConnection.get(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) { rs.next(); return rs.getInt(1); }
    }
    static void exec(String sql) throws SQLException {
        try (Connection c = DBConnection.get(); Statement s = c.createStatement()) { s.executeUpdate(sql); }
    }
    static void done() { System.out.println("\n" + pass + " passed, " + fail + " failed"); }
}
