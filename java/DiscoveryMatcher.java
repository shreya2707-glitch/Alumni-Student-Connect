public class DiscoveryMatcher implements MatchStrategy {
    public int score(Student s, Alumni a) { return MatchEngine.calculate(s, a); }
}
