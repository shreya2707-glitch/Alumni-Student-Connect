public class Admin extends User {
    public Admin(int id, String username, String fullName, String email) {
        super(id, username, fullName, email, Role.ADMIN, "", "");
    }
    @Override public String getSummary() { return "Administrator"; }
    @Override public String getWelcomeMessage() { return "Admin console - " + fullName; }
}
