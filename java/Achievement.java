/** Member 5: a badge earned by a user (table: achievements). */
public class Achievement {
    private final int userId;
    private final String userName, badge, awardedAt;
    public Achievement(int userId, String userName, String badge, String awardedAt) {
        this.userId = userId; this.userName = userName; this.badge = badge; this.awardedAt = awardedAt;
    }
    public int getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getBadge() { return badge; }
    public String getAwardedAt() { return awardedAt; }
}
