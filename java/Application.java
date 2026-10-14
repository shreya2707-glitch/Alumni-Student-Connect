/** Member 3: one student's application to one job (table: applications). */
public class Application {
    public static final String APPLIED = "APPLIED", SHORTLISTED = "SHORTLISTED",
                               INTERVIEW = "INTERVIEW", REJECTED = "REJECTED";
    public static final String[] STATUSES = { APPLIED, SHORTLISTED, INTERVIEW, REJECTED };

    public static boolean isValidStatus(String s) {
        for (String x : STATUSES) if (x.equals(s)) return true;
        return false;
    }

    private final int id, studentId, jobId;
    private final String appliedAt, jobTitle, company;
    private String status, notes;

    public Application(int id, int studentId, int jobId, String status, String appliedAt,
                       String notes, String jobTitle, String company) {
        this.id = id; this.studentId = studentId; this.jobId = jobId;
        this.status = status; this.appliedAt = nz(appliedAt); this.notes = nz(notes);
        this.jobTitle = nz(jobTitle); this.company = nz(company);
    }
    private static String nz(String s) { return s == null ? "" : s; }

    public int getId() { return id; }
    public int getStudentId() { return studentId; }
    public int getJobId() { return jobId; }
    public String getStatus() { return status; }
    public void setStatus(String s) { status = s; }
    public String getNotes() { return notes; }
    public void setNotes(String n) { notes = nz(n); }
    public String getAppliedAt() { return appliedAt; }
    public String getJobTitle() { return jobTitle; }
    public String getCompany() { return company; }
}
