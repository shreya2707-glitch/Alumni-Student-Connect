/** Member 5: used by AuthService.login so a user blocked from Moderation cannot sign in.
 *  Fails OPEN (returns false) when MySQL is not used or the blocked_users table does not exist yet,
 *  so Members 1-4's app keeps working even if M5_SCHEMA.sql has not been run. */
public final class BlockCheck {
    private BlockCheck() {}
    public static boolean isBlocked(int userId) {
        if (!AppContext.usingDatabase) return false;
        try { return new ReportRepository().isBlocked(userId); }
        catch (Exception e) { return false; }
    }
}
