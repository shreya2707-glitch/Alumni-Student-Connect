public class Alumni extends User {
    private String company, jobRole, industry, goal, qualification, journey;
    private int gradYear;

    public Alumni(int id, String username, String fullName, String email,
                  String company, String jobRole, String industry, int gradYear,
                  String skills, String interests, String goal, String qualification, String journey) {
        super(id, username, fullName, email, Role.ALUMNI, skills, interests);
        this.company = nz(company); this.jobRole = nz(jobRole); this.industry = nz(industry);
        this.gradYear = gradYear; this.goal = nz(goal);
        this.qualification = nz(qualification); this.journey = nz(journey);
    }
    private static String nz(String s) { return s == null ? "" : s; }

    public String getCompany() { return company; }       public void setCompany(String v) { company = nz(v); }
    public String getJobRole() { return jobRole; }       public void setJobRole(String v) { jobRole = nz(v); }
    public String getIndustry() { return industry; }     public void setIndustry(String v) { industry = nz(v); }
    public int getGradYear() { return gradYear; }        public void setGradYear(int v) { gradYear = v; }
    public String getGoal() { return goal; }             public void setGoal(String v) { goal = nz(v); }
    public String getQualification() { return qualification; } public void setQualification(String v) { qualification = nz(v); }
    public String getJourney() { return journey; }       public void setJourney(String v) { journey = nz(v); }

    @Override public String getSummary() {
        return jobRole + " at " + company + " (" + industry + ", Class of " + gradYear + ")";
    }
    @Override public String getWelcomeMessage() { return "Welcome, " + fullName + "! Students can find you in discovery."; }
}
