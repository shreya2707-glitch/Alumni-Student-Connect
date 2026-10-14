import java.util.List;

/** Member 5: Community Impact Score + badge rules. Pure logic (no DB), so it is easy to test. */
public final class ImpactCalculator {
    public static final int W_MENTORSHIP = 20, W_HELPFUL_ANSWER = 10, W_REFERRAL = 15, W_EVENT = 5;
    private ImpactCalculator() {}

    public static int score(int mentorships, int helpfulAnswers, int referrals, int events) {
        if (mentorships < 0 || helpfulAnswers < 0 || referrals < 0 || events < 0)
            throw new IllegalArgumentException("Activity counts cannot be negative");
        return mentorships * W_MENTORSHIP + helpfulAnswers * W_HELPFUL_ANSWER
             + referrals * W_REFERRAL + events * W_EVENT;
    }

    /** Highest badge whose min score is reached, or "None". */
    public static String badgeFor(int score, List<Badge> badges) {
        String best = "None"; int bestMin = -1;
        for (Badge b : badges)
            if (score >= b.getMinScore() && b.getMinScore() > bestMin) { best = b.getName(); bestMin = b.getMinScore(); }
        return best;
    }
}
