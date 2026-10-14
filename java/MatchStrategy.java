/** CONTRACT: Member 2 can plug in a better matching algorithm via AppContext.matcher. */
public interface MatchStrategy {
    int score(Student student, Alumni alumni);   // 0-100
}
