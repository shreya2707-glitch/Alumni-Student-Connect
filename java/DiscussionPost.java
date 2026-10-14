import java.sql.Timestamp;

public class DiscussionPost {
    private int id;
    private int circleId;
    private int userId;
    private String content;
    private Timestamp createdAt;

    public DiscussionPost() {}

    public DiscussionPost(int id, int circleId, int userId, String content, Timestamp createdAt) {
        this.id = id;
        this.circleId = circleId;
        this.userId = userId;
        this.content = content;
        this.createdAt = createdAt;
    }

    public DiscussionPost(int circleId, int userId, String content) {
        this(0, circleId, userId, content, new Timestamp(System.currentTimeMillis()));
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCircleId() { return circleId; }
    public void setCircleId(int circleId) { this.circleId = circleId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}
