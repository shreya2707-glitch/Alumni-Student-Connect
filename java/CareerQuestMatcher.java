import java.util.*;

/** Member 2: mentor matching that considers skills, interests, target industry and career goal. */
public class CareerQuestMatcher implements MatchStrategy {
    @Override
    public int score(Student student, Alumni alumni) {
        if (student == null || alumni == null) return 0;
        double skillScore = overlap(student.getSkills(), alumni.getSkills());
        double interestScore = overlap(student.getInterests(), alumni.getInterests());
        double industryScore = same(student.getTargetIndustry(), alumni.getIndustry()) ? 1.0 : 0.0;
        double goalScore = same(student.getGoal(), alumni.getGoal()) ? 1.0 : 0.0;
        // Give skills the highest weight because they are most useful for career mentoring.
        int score = (int) Math.round(skillScore * 40 + industryScore * 25 + interestScore * 20 + goalScore * 15);
        return Math.max(0, Math.min(100, score));
    }

    private double overlap(List<String> first, List<String> second) {
        if (first == null || first.isEmpty() || second == null || second.isEmpty()) return 0.0;
        Set<String> normalized = new HashSet<>();
        for (String item : second) if (item != null) normalized.add(item.trim().toLowerCase(Locale.ROOT));
        int matches = 0;
        for (String item : first) {
            if (item != null && normalized.contains(item.trim().toLowerCase(Locale.ROOT))) matches++;
        }
        return Math.min(1.0, (double) matches / first.size());
    }

    private boolean same(String first, String second) {
        return first != null && second != null && !first.trim().isEmpty()
            && first.trim().equalsIgnoreCase(second.trim());
    }
}
