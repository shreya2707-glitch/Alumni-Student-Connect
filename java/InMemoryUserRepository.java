import java.util.*;

/** Fallback repository so the app runs with no database. */
public class InMemoryUserRepository implements UserRepository, AlumniRepository {
    private final Map<Integer, User> users = new LinkedHashMap<>();
    private final Map<String, String> hashes = new HashMap<>();
    private int nextId = 1;

    public User findById(int id) { return users.get(id); }
    public User findByUsername(String name) {
        for (User u : users.values()) if (u.getUsername().equalsIgnoreCase(name)) return u;
        return null;
    }
    public boolean usernameExists(String name) { return findByUsername(name) != null; }
    public String getPasswordHash(String name) {
        User u = findByUsername(name);
        return u == null ? null : hashes.get(u.getUsername());
    }
    public User save(User u, String hash) {
        if (u.getId() == 0) u.setId(nextId++);
        users.put(u.getId(), u);
        hashes.put(u.getUsername(), hash);
        return u;
    }
    public void updateProfile(User u) { users.put(u.getId(), u); }
    public List<User> findAllUsers() { return new ArrayList<>(users.values()); }

    public List<Alumni> findAllAlumni() {
        List<Alumni> out = new ArrayList<>();
        for (User u : users.values()) if (u instanceof Alumni) out.add((Alumni) u);
        return out;
    }
    public List<Alumni> findAlumniByIndustry(String ind) {
        List<Alumni> out = new ArrayList<>();
        for (Alumni a : findAllAlumni()) if (a.getIndustry().equalsIgnoreCase(ind)) out.add(a);
        return out;
    }
    public Alumni findAlumniById(int id) {
        User u = users.get(id);
        return u instanceof Alumni ? (Alumni) u : null;
    }
}
