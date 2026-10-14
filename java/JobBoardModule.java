import javax.swing.JComponent;

/** Member 3: adds the Opportunity Arena tab to the shared application. */
public class JobBoardModule implements FeatureModule {
    @Override public String getTitle() { return "Opportunity Arena"; }

    @Override public boolean isVisibleFor(Role role) {
        return role == Role.STUDENT || role == Role.ALUMNI;
    }

    @Override public JComponent createPanel(User currentUser) {
        return new JobBoardPanel(currentUser);
    }
}
