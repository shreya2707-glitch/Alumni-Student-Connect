import java.util.*;

/**
 * INTEGRATION POINT shared by all members.
 *   AppContext.users   -> UserRepository       AppContext.alumni -> AlumniRepository
 *   AppContext.auth    -> AuthService          AppContext.matcher -> MatchStrategy (Member 2 can replace)
 *   AppContext.modules -> register your screens here
 */
public class AppContext {
    public static UserRepository users;
    public static AlumniRepository alumni;
    public static AuthService auth;
    public static MatchStrategy matcher = new DiscoveryMatcher();
    public static boolean usingDatabase;
    public static final List<FeatureModule> modules = new ArrayList<>();

    public static void init() {
        if (DBConnection.isAvailable()) {
            JdbcUserRepository r = new JdbcUserRepository();
            users = r; alumni = r; usingDatabase = true;
        } else {
            InMemoryUserRepository r = new InMemoryUserRepository();
            users = r; alumni = r; usingDatabase = false;
        }
        if (users.findAllUsers().isEmpty()) SampleData.seed(users);
        auth = new AuthService(users);
        // Member 2: improved mentor matching and mentorship/career quest module.
        matcher = new CareerQuestMatcher();
        modules.add(new MentorshipModule());
        modules.add(new JobBoardModule());          // Member 3
        modules.add(new CampusConnectModule());     // Member 4
        modules.add(new AdminModule());             // Member 5
    }
}
