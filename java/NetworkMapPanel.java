import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/** Visual network map: alumni grouped by company, industry or graduation year. */
public class NetworkMapPanel extends JPanel {
    private static class Node { Alumni a; int x, y; }

    private final MainFrame frame;
    private final JComboBox<String> groupBy = new JComboBox<>(new String[]{"Company", "Industry", "Graduation year"});
    private final MapCanvas canvas = new MapCanvas();
    private final List<Node> nodes = new ArrayList<>();

    public NetworkMapPanel(MainFrame frame) {
        this.frame = frame;
        setLayout(new BorderLayout());
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Group alumni by:")); top.add(groupBy);
        top.add(new JLabel("   (greener = better match; hover for name, click for profile)"));
        add(top, BorderLayout.NORTH);
        add(canvas, BorderLayout.CENTER);
        groupBy.addActionListener(e -> canvas.repaint());
    }

    @Override public void repaint() { super.repaint(); if (canvas != null) canvas.repaint(); }

    private String keyOf(Alumni a) {
        switch (groupBy.getSelectedIndex()) {
            case 0: return a.getCompany();
            case 1: return a.getIndustry();
            default: return "" + a.getGradYear();
        }
    }

    private class MapCanvas extends JPanel {
        MapCanvas() {
            setBackground(Color.WHITE);
            addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseMoved(MouseEvent e) {
                    Node n = hit(e.getX(), e.getY());
                    setToolTipText(n == null ? null : n.a.getName() + " - " + n.a.getJobRole());
                }
            });
            addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    Node n = hit(e.getX(), e.getY());
                    if (n != null) DashboardPanel.showDetails(n.a);
                }
            });
        }

        Node hit(int x, int y) {
            for (Node n : nodes) if (Math.hypot(n.x - x, n.y - y) <= 12) return n;
            return null;
        }

        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            nodes.clear();

            Map<String, List<Alumni>> groups = new TreeMap<>();
            for (Alumni a : frame.all) groups.computeIfAbsent(keyOf(a), k -> new ArrayList<>()).add(a);

            int w = getWidth(), h = getHeight(), n = groups.size(), i = 0;
            double R = Math.min(w, h) / 2.0 - 90;
            for (Map.Entry<String, List<Alumni>> e : groups.entrySet()) {
                double ang = 2 * Math.PI * i++ / n;
                int cx = (int) (w / 2 + R * Math.cos(ang)), cy = (int) (h / 2 + R * Math.sin(ang));
                List<Alumni> members = e.getValue();
                // lines to centre hub and members
                g.setColor(new Color(210, 215, 230));
                g.drawLine(w / 2, h / 2, cx, cy);
                for (int j = 0; j < members.size(); j++) {
                    double a2 = ang + 2 * Math.PI * j / members.size();   // first member points away from centre
                    int mx = (int) (cx + 55 * Math.cos(a2)), my = (int) (cy + 55 * Math.sin(a2));
                    g.setColor(new Color(180, 190, 215));
                    g.drawLine(cx, cy, mx, my);
                    Node nd = new Node(); nd.a = members.get(j); nd.x = mx; nd.y = my;
                    nodes.add(nd);
                }
                g.setColor(new Color(60, 80, 160));
                g.fillOval(cx - 9, cy - 9, 18, 18);
                g.setColor(Color.DARK_GRAY);
                g.setFont(new Font("SansSerif", Font.BOLD, 12));
                FontMetrics fm = g.getFontMetrics();
                g.drawString(e.getKey(), cx - fm.stringWidth(e.getKey()) / 2, cy - 14);   // label sits on its hub
            }
            g.setColor(new Color(60, 80, 160));
            g.fillOval(w / 2 - 22, h / 2 - 22, 44, 44);
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 11));
            g.drawString("Alumni", w / 2 - 20, h / 2 + 4);

            // alumni nodes (colour = match score)
            for (Node nd : nodes) {
                int s = frame.scoreOf(nd.a);
                g.setColor(!frame.hasMatchData() ? new Color(90, 150, 230)
                    : new Color(220 - s * 2, 90 + (int) (s * 1.5), 90));
                g.fillOval(nd.x - 10, nd.y - 10, 20, 20);
                g.setColor(Color.DARK_GRAY);
                g.drawOval(nd.x - 10, nd.y - 10, 20, 20);
                g.setFont(new Font("SansSerif", Font.PLAIN, 10));
                g.drawString(nd.a.getName().split(" ")[0], nd.x - 14, nd.y + 24);
            }
        }
    }
}
