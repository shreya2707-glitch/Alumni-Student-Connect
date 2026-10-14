public class AuthService {
    private final UserRepository repo;
    public AuthService(UserRepository repo) { this.repo = repo; }

    public User register(User u, String password) throws AuthException {
        if (u.getUsername() == null || u.getUsername().trim().length() < 3)
            throw new AuthException("Username must be at least 3 characters.");
        if (password == null || password.length() < 6)
            throw new AuthException("Password must be at least 6 characters.");
        if (u.getFullName() == null || u.getFullName().trim().isEmpty())
            throw new AuthException("Full name is required.");
        if (u.getEmail() == null || !u.getEmail().matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+"))
            throw new AuthException("Enter a valid email address.");
        if (u.getType() == Role.ADMIN)
            throw new AuthException("Admin accounts cannot be self-registered.");
        if (repo.usernameExists(u.getUsername()))
            throw new AuthException("That username is already taken.");
        return repo.save(u, PasswordUtil.hash(password));
    }

    public User login(String username, String password) throws AuthException {
        String name = username == null ? "" : username.trim();
        String stored = repo.getPasswordHash(name);
        if (stored == null || !PasswordUtil.verify(password, stored))
            throw new AuthException("Invalid username or password.");
        User u = repo.findByUsername(name);
        if (BlockCheck.isBlocked(u.getId()))                                   // Member 5: blocked by an admin
            throw new AuthException("Your account has been blocked by an administrator.");
        Session.start(u);
        return u;
    }

    public void logout() { Session.end(); }
}
