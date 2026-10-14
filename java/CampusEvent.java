public class CampusEvent {
    private int id;
    private String title;
    private String eventDate;
    private int seatsLeft;

    public CampusEvent() {}

    public CampusEvent(int id, String title, String eventDate, int seatsLeft) {
        this.id = id;
        this.title = title;
        this.eventDate = eventDate;
        this.seatsLeft = seatsLeft;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getEventDate() { return eventDate; }
    public void setEventDate(String eventDate) { this.eventDate = eventDate; }

    public int getSeatsLeft() { return seatsLeft; }
    public void setSeatsLeft(int seatsLeft) { this.seatsLeft = seatsLeft; }

    @Override
    public String toString() {
        return title + " (" + seatsLeft + " seats left)";
    }
}
