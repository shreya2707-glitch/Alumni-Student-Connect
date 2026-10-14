/** Holds the logged-in user. Any member's code can call Session.current(). */
public final class Session {
    private static User current;
    private Session() {}
    public static void start(User u) { current = u; }
    public static void end() { current = null; }
    public static User current() { return current; }
    public static boolean isLoggedIn() { return current != null; }
    public static boolean hasRole(Role r) { return current != null && current.getType() == r; }
}
