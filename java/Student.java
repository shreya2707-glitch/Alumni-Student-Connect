public class Student extends User {
    private String targetIndustry, goal;

    public Student(int id, String username, String fullName, String email,
                   String skills, String interests, String targetIndustry, String goal) {
        super(id, username, fullName, email, Role.STUDENT, skills, interests);
        this.targetIndustry = targetIndustry == null ? "" : targetIndustry;
        this.goal = goal == null ? "" : goal;
    }
    public String getTargetIndustry() { return targetIndustry; }
    public void setTargetIndustry(String t) { targetIndustry = t == null ? "" : t; }
    public String getGoal() { return goal; }
    public void setGoal(String g) { goal = g == null ? "" : g; }

    @Override public String getSummary() { return "Student aiming for " + targetIndustry + " as a " + goal; }
    @Override public String getWelcomeMessage() { return "Welcome back, " + fullName + "! Ready to find a mentor?"; }
}
