import java.util.List;

/** Member 3: one job or internship listing (table: jobs). */
public class Job {
    public static final String JOB = "JOB", INTERNSHIP = "INTERNSHIP";

    private final int id, postedBy;
    private final String title, company, jobType, location, requiredSkillsCsv, description, createdAt;

    public Job(int id, String title, String company, String jobType, String location,
               String requiredSkillsCsv, String description, int postedBy, String createdAt) {
        this.id = id; this.title = nz(title); this.company = nz(company);
        this.jobType = INTERNSHIP.equals(jobType) ? INTERNSHIP : JOB;
        this.location = nz(location); this.requiredSkillsCsv = nz(requiredSkillsCsv);
        this.description = nz(description); this.postedBy = postedBy; this.createdAt = nz(createdAt);
    }
    private static String nz(String s) { return s == null ? "" : s; }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getCompany() { return company; }
    public String getJobType() { return jobType; }
    public String getLocation() { return location; }
    public String getRequiredSkillsCsv() { return requiredSkillsCsv; }
    public List<String> getRequiredSkills() { return User.split(requiredSkillsCsv); }
    public String getDescription() { return description; }
    public int getPostedBy() { return postedBy; }
    public String getCreatedAt() { return createdAt; }

    @Override public String toString() { return title + " - " + company + " (" + jobType + ")"; }
}
