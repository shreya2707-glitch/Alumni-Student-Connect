import java.util.List;

/** CONTRACT: all user persistence goes through this interface. */
public interface UserRepository {
    User findById(int id);
    User findByUsername(String username);
    boolean usernameExists(String username);
    String getPasswordHash(String username);       // used only by AuthService
    User save(User newUser, String passwordHash);  // assigns and returns the new id
    void updateProfile(User user);
    List<User> findAllUsers();                     // for admin moderation / analytics
}
