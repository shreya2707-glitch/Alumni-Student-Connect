/** Member 5: user feedback (table: feedback). */
public class Feedback {
    public static final String NEW = "NEW", REVIEWED = "REVIEWED", ACTIONED = "ACTIONED";
    private final int id, userId, rating;
    private final String userName, message, status, createdAt;
    public Feedback(int id, int userId, String userName, String message, int rating, String status, String createdAt) {
        this.id = id; this.userId = userId; this.userName = userName; this.message = message;
        this.rating = rating; this.status = status; this.createdAt = createdAt;
    }
    public int getId() { return id; }
    public int getUserId() { return userId; }
    public String getUserName() { return userName; }
    public String getMessage() { return message; }
    public int getRating() { return rating; }
    public String getStatus() { return status; }
    public String getCreatedAt() { return createdAt; }
}
