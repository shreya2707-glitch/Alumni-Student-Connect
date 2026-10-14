/** Member 3: a student's request for an alumnus to refer them for a job (table: referrals). */
public class Referral {
    public static final String PENDING = "PENDING", ACCEPTED = "ACCEPTED", REJECTED = "REJECTED";

    private final int id, studentId, alumniId, jobId;
    private final String message, createdAt;
    private String status;

    public Referral(int id, int studentId, int alumniId, int jobId, String message,
                    String status, String createdAt) {
        this.id = id; this.studentId = studentId; this.alumniId = alumniId; this.jobId = jobId;
        this.message = message == null ? "" : message; this.status = status;
        this.createdAt = createdAt == null ? "" : createdAt;
    }

    public int getId() { return id; }
    public int getStudentId() { return studentId; }
    public int getAlumniId() { return alumniId; }
    public int getJobId() { return jobId; }
    public String getMessage() { return message; }
    public String getStatus() { return status; }
    public void setStatus(String s) { status = s; }
    public String getCreatedAt() { return createdAt; }
}
