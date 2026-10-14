/** Member 5: a badge level (table: badges). */
public class Badge {
    private final int id, minScore;
    private final String name;
    public Badge(int id, String name, int minScore) { this.id = id; this.name = name; this.minScore = minScore; }
    public int getId() { return id; }
    public String getName() { return name; }
    public int getMinScore() { return minScore; }
}
