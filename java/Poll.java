public class Poll {
    private int id;
    private int eventId;
    private String question;

    public Poll() {}

    public Poll(int id, int eventId, String question) {
        this.id = id;
        this.eventId = eventId;
        this.question = question;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getEventId() { return eventId; }
    public void setEventId(int eventId) { this.eventId = eventId; }

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    @Override
    public String toString() {
        return question;
    }
}
