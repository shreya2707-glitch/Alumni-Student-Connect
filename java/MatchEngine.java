import java.util.*;

/** Discovery match score + helpers. */
public class MatchEngine {

    public static List<String> shared(List<String> a, List<String> b) {
        List<String> out = new ArrayList<>();
        for (String x : a) for (String y : b) if (x.equalsIgnoreCase(y)) { out.add(x); break; }
        return out;
    }

    private static double overlap(List<String> student, List<String> alumni) {
        if (student.isEmpty()) return 0;
        return (double) shared(student, alumni).size() / student.size();
    }

    /** Skills 40, industry 25, interests 20, goal 15. */
    public static int calculate(Student s, Alumni a) {
        double skill = overlap(s.getSkills(), a.getSkills());
        double interest = overlap(s.getInterests(), a.getInterests());
        double industry = s.getTargetIndustry().equalsIgnoreCase(a.getIndustry()) ? 1 : 0;
        double goal = s.getGoal().equalsIgnoreCase(a.getGoal()) ? 1 : 0;
        return (int) Math.round(skill * 40 + industry * 25 + interest * 20 + goal * 15);
    }

    public static ArrayList<Alumni> rank(Student s, List<Alumni> all, MatchStrategy m) {
        ArrayList<Alumni> r = new ArrayList<>(all);
        r.sort((x, y) -> Integer.compare(m.score(s, y), m.score(s, x)));
        return r;
    }

    /** Career DNA: most common job role among the top 3 matches. */
    public static String careerDNA(Student s, List<Alumni> all, MatchStrategy m) {
        ArrayList<Alumni> r = rank(s, all, m);
        Map<String, Integer> count = new LinkedHashMap<>();
        for (int i = 0; i < Math.min(3, r.size()); i++) count.merge(r.get(i).getJobRole(), 1, Integer::sum);
        String best = ""; int max = 0;
        for (Map.Entry<String, Integer> e : count.entrySet())
            if (e.getValue() > max) { max = e.getValue(); best = e.getKey(); }
        return best;
    }
}
