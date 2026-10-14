import java.util.*;

/** Root of the user hierarchy. Every member's code should depend on this class. */
public abstract class User {
    protected int id;
    protected String username, fullName, email, bio = "";
    protected final Role type;
    protected List<String> skills, interests;

    protected User(int id, String username, String fullName, String email,
                   Role type, String skillsCsv, String interestsCsv) {
        this.id = id; this.username = username; this.fullName = fullName;
        this.email = email; this.type = type;
        this.skills = split(skillsCsv);
        this.interests = split(interestsCsv);
    }

    public static List<String> split(String csv) {
        List<String> out = new ArrayList<>();
        if (csv == null || csv.trim().isEmpty()) return out;
        for (String s : csv.split(",")) if (!s.trim().isEmpty()) out.add(s.trim());
        return out;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getUsername() { return username; }
    public String getFullName() { return fullName; }
    public String getName() { return fullName; }
    public void setFullName(String n) { fullName = n; }
    public String getEmail() { return email; }
    public void setEmail(String e) { email = e; }
    public String getBio() { return bio; }
    public void setBio(String b) { bio = b == null ? "" : b; }
    public Role getType() { return type; }
    public List<String> getSkills() { return skills; }
    public List<String> getInterests() { return interests; }
    public String getSkillsCsv() { return String.join(",", skills); }
    public String getInterestsCsv() { return String.join(",", interests); }
    public void setSkillsCsv(String csv) { skills = split(csv); }
    public void setInterestsCsv(String csv) { interests = split(csv); }

    public abstract String getSummary();                 // polymorphic
    public String getWelcomeMessage() { return "Welcome, " + fullName + "!"; }

    @Override public String toString() { return fullName; }
}
