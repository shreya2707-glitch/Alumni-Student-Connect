import java.util.*;

/**
 * Member 3: simple rule-based skill matching (no AI, no external API).
 * Used by "Why am I a match?", the Skill Gap Checker, the Resume Checklist and referral eligibility.
 * Score = matched required skills / total required skills x 100.
 */
public final class SkillMatcher {
    private SkillMatcher() {}

    public static class Result {
        private final List<String> matched, missing;
        private final boolean profileHasSkills;

        Result(List<String> matched, List<String> missing, boolean profileHasSkills) {
            this.matched = matched; this.missing = missing; this.profileHasSkills = profileHasSkills;
        }
        public List<String> getMatched() { return matched; }
        public List<String> getMissing() { return missing; }
        public int getTotalRequired() { return matched.size() + missing.size(); }
        public int getPercent() {
            int total = getTotalRequired();
            return total == 0 ? 0 : (int) Math.round(100.0 * matched.size() / total);
        }

        /** Plain-English explanation of the score. */
        public String getExplanation() {
            int total = getTotalRequired();
            if (total == 0) return "This opportunity does not list any required skills, so a score cannot be calculated.";
            if (!profileHasSkills) return "Your profile has no skills yet. Add them in the My Profile tab, then check again.";
            int p = getPercent();
            String verdict = p >= 80 ? "Strong match - you are ready to apply."
                           : p >= 50 ? "Good match - learning the missing skills will make you a stronger candidate."
                           : "Low match - focus on the missing skills before applying.";
            return "You currently match " + matched.size() + " out of " + total + " required skills.\n"
                 + "Score = matched skills / required skills x 100 = " + p + "%.\n" + verdict;
        }
    }

    /** Compares skills ignoring case and extra spaces; duplicates in the requirement list count once. */
    public static Result compare(List<String> studentSkills, List<String> requiredSkills) {
        Set<String> have = new HashSet<>();
        if (studentSkills != null)
            for (String s : studentSkills) if (s != null && !s.trim().isEmpty()) have.add(norm(s));
        List<String> matched = new ArrayList<>(), missing = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        if (requiredSkills != null) {
            for (String r : requiredSkills) {
                if (r == null || r.trim().isEmpty() || !seen.add(norm(r))) continue;
                if (have.contains(norm(r))) matched.add(r.trim()); else missing.add(r.trim());
            }
        }
        return new Result(matched, missing, !have.isEmpty());
    }

    private static String norm(String s) { return s.trim().toLowerCase(Locale.ROOT); }
}
