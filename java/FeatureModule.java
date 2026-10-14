import javax.swing.JComponent;

/** CONTRACT: each member registers screens with AppContext.modules.add(...). */
public interface FeatureModule {
    String getTitle();
    boolean isVisibleFor(Role role);
    JComponent createPanel(User currentUser);
}
