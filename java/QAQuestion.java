import java.sql.Timestamp;

public class QAQuestion {
    private int id;
    private int userId;
    private String title;
    private String details;
    private Timestamp createdAt;

    public QAQuestion() {}

    public QAQuestion(int id, int userId, String title, String details, Timestamp createdAt) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.details = details;
        this.createdAt = createdAt;
    }

    public QAQuestion(int userId, String title, String details) {
        this(0, userId, title, details, new Timestamp(System.currentTimeMillis()));
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return "#" + id + ": " + title;
    }
}
