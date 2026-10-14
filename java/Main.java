import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        AppContext.init();
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
