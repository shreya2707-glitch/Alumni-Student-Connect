import javax.swing.JComponent;

/**
 * CampusConnectModule - Pluggable integration bridge for Member 1's AppContext.
 * Allows 1-line integration: AppContext.modules.add(new CampusConnectModule());
 */
public class CampusConnectModule implements FeatureModule {

    private CampusConnectRepository sharedRepo;

    public CampusConnectModule() {}

    @Override
    public String getTitle() {
        return "Campus Connect & Activities";
    }

    @Override
    public boolean isVisibleFor(Role role) {
        return role == Role.STUDENT || role == Role.ALUMNI;
    }

    @Override
    public JComponent createPanel(User currentUser) {
        if (sharedRepo == null) {
            sharedRepo = new CampusConnectRepository();
        }
        int id = currentUser != null ? currentUser.getId() : 1;
        String roleStr = (currentUser != null && currentUser.getType() != null)
                ? currentUser.getType().name() : "STUDENT";
        return new CampusConnectPanel(id, roleStr, sharedRepo);
    }
}
