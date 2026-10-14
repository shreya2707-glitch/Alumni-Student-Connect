import javax.swing.JComponent;

/** Adds the Mentorship & Career Quest screen to the shared application. */
public class MentorshipModule implements FeatureModule {
    @Override public String getTitle() { return "Mentorship & Career Quest"; }

    @Override public boolean isVisibleFor(Role role) {
        return role == Role.STUDENT || role == Role.ALUMNI;
    }

    @Override public JComponent createPanel(User currentUser) {
        return new MentorshipPanel(currentUser);
    }
}
