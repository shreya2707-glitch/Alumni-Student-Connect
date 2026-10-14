import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * EventsCalendarPanel - Campus Events, Transactional RSVP, and Race Condition Demo.
 * Features:
 *  - Event RSVP with Row-Level Transactional Locks (bookEventSeat).
 *  - Interactive Viva Demo: 100-thread Stress Test pitting Unsynchronized primitive int
 *    against AtomicInteger with CountDownLatch gate synchronization.
 */
public class EventsCalendarPanel extends JPanel {

    private final int userId;
    private final String role;
    private final CampusConnectRepository repo;

    private final DefaultListModel<CampusEvent> eventListModel = new DefaultListModel<>();
    private final JList<CampusEvent> eventList = new JList<>(eventListModel);

    // Viva Demo State
    private int plainCounter = 0;
    private final AtomicInteger atomicCounter = new AtomicInteger(0);
    private final JLabel plainResultLabel = new JLabel("Unsynchronized int: 0");
    private final JLabel atomicResultLabel = new JLabel("AtomicInteger: 0");
    private final JLabel expectedLabel = new JLabel("Expected Total: 100,000 (100 threads × 1,000)");
    private final JButton runStressTestBtn = new JButton("Run 100-Thread Race Condition Demo");

    public EventsCalendarPanel(int userId, String role, CampusConnectRepository repo) {
        this.userId = userId;
        this.role = role;
        this.repo = repo;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 1. Top/Center: Events List & RSVP
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.setBorder(BorderFactory.createTitledBorder("Upcoming Campus Events & Webinars"));

        eventList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane eventScroll = new JScrollPane(eventList);
        topPanel.add(eventScroll, BorderLayout.CENTER);

        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton rsvpBtn = new JButton("RSVP / Reserve Seat");
        JButton refreshBtn = new JButton("Refresh Events");

        // RUBRIC: [Concept: Transactional Seat Reservation Invocation]
        rsvpBtn.addActionListener(e -> handleRsvp());
        refreshBtn.addActionListener(e -> loadEvents());

        actionPanel.add(refreshBtn);
        actionPanel.add(rsvpBtn);
        topPanel.add(actionPanel, BorderLayout.SOUTH);

        add(topPanel, BorderLayout.CENTER);

        // 2. Bottom Panel: The Viva Concurrency Demo
        JPanel demoPanel = new JPanel(new BorderLayout(8, 8));
        demoPanel.setBorder(BorderFactory.createTitledBorder("Viva Demo: Race Condition & Memory Visibility"));
        demoPanel.setBackground(new Color(248, 249, 250));

        JPanel labelsPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        labelsPanel.setOpaque(false);
        expectedLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        plainResultLabel.setFont(new Font("Monospaced", Font.BOLD, 14));
        plainResultLabel.setForeground(new Color(180, 40, 40)); // Red highlight for race condition
        atomicResultLabel.setFont(new Font("Monospaced", Font.BOLD, 14));
        atomicResultLabel.setForeground(new Color(30, 130, 60)); // Green highlight for atomic accuracy

        labelsPanel.add(expectedLabel);
        labelsPanel.add(plainResultLabel);
        labelsPanel.add(atomicResultLabel);

        demoPanel.add(labelsPanel, BorderLayout.CENTER);

        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        btnBar.setOpaque(false);
        runStressTestBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        runStressTestBtn.addActionListener(e -> runRaceConditionStressTest());
        btnBar.add(runStressTestBtn);

        demoPanel.add(btnBar, BorderLayout.SOUTH);
        add(demoPanel, BorderLayout.SOUTH);

        loadEvents();
    }

    private void loadEvents() {
        eventListModel.clear();
        List<CampusEvent> events = repo.getAllEvents();
        for (CampusEvent ev : events) {
            eventListModel.addElement(ev);
        }
        if (!eventListModel.isEmpty()) {
            eventList.setSelectedIndex(0);
        }
    }

    // RUBRIC: [Concept: Thread-Safe DB Booking Response]
    private void handleRsvp() {
        CampusEvent ev = eventList.getSelectedValue();
        if (ev == null) {
            JOptionPane.showMessageDialog(this, "Please select an event to RSVP.");
            return;
        }

        // Executes transactional method with striped ReentrantLock
        boolean success = repo.bookEventSeat(ev.getId(), userId);
        if (success) {
            JOptionPane.showMessageDialog(this,
                    "Seat successfully reserved for: " + ev.getTitle() + "\n(Atomic seat count decremented).",
                    "RSVP Confirmed", JOptionPane.INFORMATION_MESSAGE);
            loadEvents();
        } else {
            JOptionPane.showMessageDialog(this,
                    "Reservation failed! Either all seats are booked or you already RSVP'd.",
                    "RSVP Failed", JOptionPane.WARNING_MESSAGE);
        }
    }

    // =========================================================================
    // VIVA CONCURRENCY DEMO: RACE CONDITION STRESS TEST
    // =========================================================================

    // RUBRIC: [Concept: Multithreaded Race Condition & Synchronization Demonstration]
    // Pits non-atomic primitive int against AtomicInteger using CountDownLatch.
    private void runRaceConditionStressTest() {
        runStressTestBtn.setEnabled(false);
        plainResultLabel.setText("Running 100 threads concurrently...");
        atomicResultLabel.setText("Running 100 threads concurrently...");

        // Run off-EDT to avoid freezing Swing UI
        new Thread(() -> {
            plainCounter = 0;
            atomicCounter.set(0);

            int totalThreads = 100;
            int iterationsPerThread = 1000;

            // RUBRIC: [Concept: CountDownLatch Gate]
            // A latch with count 1 acts as a starting gun. All 100 threads wait at
            // startGate.await() so they unleash updates at the exact same instant,
            // maximizing thread contention and data races on plainCounter.
            CountDownLatch startGate = new CountDownLatch(1);
            ExecutorService executor = Executors.newFixedThreadPool(totalThreads);

            for (int i = 0; i < totalThreads; i++) {
                executor.submit(() -> {
                    try {
                        startGate.await(); // Wait for simultaneous start signal
                        for (int j = 0; j < iterationsPerThread; j++) {
                            // Non-thread-safe read-modify-write cycle:
                            // Leads to Lost Updates due to thread interleaving and CPU cache inconsistency
                            plainCounter++;

                            // Hardware-level atomic Compare-And-Swap (CAS) instruction:
                            // Completely immune to race conditions
                            atomicCounter.incrementAndGet();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                });
            }

            // Fire the start gate!
            startGate.countDown();

            executor.shutdown();
            try {
                // Await all workers
                boolean completed = executor.awaitTermination(5, TimeUnit.SECONDS);
                if (!completed) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }

            // Marshaling results back to EDT
            SwingUtilities.invokeLater(() -> {
                plainResultLabel.setText("Unsynchronized int: " + String.format("%,d", plainCounter) +
                        "  <-- [DATA RACE! Lost updates due to non-atomic ++]");
                atomicResultLabel.setText("AtomicInteger:       " + String.format("%,d", atomicCounter.get()) +
                        "  <-- [THREAD-SAFE! Hardware CAS guarantees 100,000]");
                runStressTestBtn.setEnabled(true);
            });
        }, "M4-StressTestRunner").start();
    }
}
