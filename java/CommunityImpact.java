/** Member 5: one leaderboard row (a row of the v_impact view). */
public class CommunityImpact {
    private final int userId, mentorships, helpfulAnswers, referrals, events;
    private final String name, role;
    public CommunityImpact(int userId, String name, String role, int mentorships, int helpfulAnswers, int referrals, int events) {
        this.userId = userId; this.name = name; this.role = role;
        this.mentorships = mentorships; this.helpfulAnswers = helpfulAnswers; this.referrals = referrals; this.events = events;
    }
    public int getUserId() { return userId; }
    public String getName() { return name; }
    public String getRole() { return role; }
    public int getMentorships() { return mentorships; }
    public int getHelpfulAnswers() { return helpfulAnswers; }
    public int getReferrals() { return referrals; }
    public int getEvents() { return events; }
    public int getTotal() { return ImpactCalculator.score(mentorships, helpfulAnswers, referrals, events); }
}
