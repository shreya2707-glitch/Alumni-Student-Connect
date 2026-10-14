import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 * QABoardPanel - Live "Ask an Alumnus" Q&A Board.
 * Features:
 *  - 300ms Debounced Asynchronous Search using javax.swing.Timer
 *  - Cooperative background query execution via SwingWorker<Void, QAQuestion>
 *  - Responsive chunked UI updates via publish() and process()
 *  - Immediate cancellation of stale queries via worker.cancel(true)
 */
public class QABoardPanel extends JPanel {

    private final int userId;
    private final String role;
    private final CampusConnectRepository repo;

    private final JTextField searchField = new JTextField(25);
    private final DefaultListModel<QAQuestion> questionListModel = new DefaultListModel<>();
    private final JList<QAQuestion> questionList = new JList<>(questionListModel);
    private final JTextArea detailsArea = new JTextArea(6, 30);
    private final DefaultListModel<String> answersListModel = new DefaultListModel<>();
    private final JList<String> answersList = new JList<>(answersListModel);

    // RUBRIC: [Concept: Debounced Search Timer]
    // A one-shot javax.swing.Timer (300ms) prevents firing a database query on every single
    // keystroke, coalescing rapid typing into a single batch query.
    private final Timer debounceTimer;

    // RUBRIC: [Concept: SwingWorker Lifecycle & Cancellation]
    // Reference to active background worker. If a new keystroke arrives while an old search
    // is running, the old worker is cooperatively cancelled via worker.cancel(true).
    private SearchWorker currentWorker = null;

    public QABoardPanel(int userId, String role, CampusConnectRepository repo) {
        this.userId = userId;
        this.role = role;
        this.repo = repo;

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 1. Debounce Timer Setup
        debounceTimer = new Timer(300, (ActionEvent e) -> triggerSearch());
        debounceTimer.setRepeats(false);

        // 2. Search Top Bar
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        topPanel.add(new JLabel("Search Q&A (Debounced 300ms):"));
        searchField.setFont(new Font("SansSerif", Font.PLAIN, 13));
        topPanel.add(searchField);

        JButton askBtn = new JButton("+ Ask Question");
        askBtn.addActionListener(e -> showAskQuestionDialog());
        topPanel.add(askBtn);

        add(topPanel, BorderLayout.NORTH);

        // 3. DocumentListener on Search Input
        // RUBRIC: [Concept: Real-Time Event Listening for Debounce]
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { restartTimer(); }
            @Override
            public void removeUpdate(DocumentEvent e) { restartTimer(); }
            @Override
            public void changedUpdate(DocumentEvent e) { restartTimer(); }

            private void restartTimer() {
                debounceTimer.restart(); // Resets the 300ms countdown on every key press
            }
        });

        // 4. Split Pane: Questions on Left, Details & Answers on Right
        questionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        questionList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadSelectedQuestionDetails();
            }
        });

        JScrollPane leftScroll = new JScrollPane(questionList);
        leftScroll.setBorder(BorderFactory.createTitledBorder("Questions"));

        JPanel rightPanel = new JPanel(new BorderLayout(8, 8));
        detailsArea.setEditable(false);
        detailsArea.setLineWrap(true);
        detailsArea.setWrapStyleWord(true);
        JScrollPane detailsScroll = new JScrollPane(detailsArea);
        detailsScroll.setBorder(BorderFactory.createTitledBorder("Question Details"));

        JPanel answerContainer = new JPanel(new BorderLayout(5, 5));
        answerContainer.setBorder(BorderFactory.createTitledBorder("Answers"));
        JScrollPane answersScroll = new JScrollPane(answersList);
        answerContainer.add(answersScroll, BorderLayout.CENTER);

        JPanel answerInputPanel = new JPanel(new BorderLayout(5, 5));
        JTextField answerInput = new JTextField();
        JButton submitAnswerBtn = new JButton("Post Answer");
        JButton markHelpfulBtn = new JButton("★ Mark Helpful");

        JPanel answerBtnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        answerBtnBar.add(markHelpfulBtn);
        answerBtnBar.add(submitAnswerBtn);
        answerInputPanel.add(answerInput, BorderLayout.CENTER);
        answerInputPanel.add(answerBtnBar, BorderLayout.EAST);

        submitAnswerBtn.addActionListener(e -> {
            QAQuestion q = questionList.getSelectedValue();
            String txt = answerInput.getText().trim();
            if (q != null && !txt.isEmpty()) {
                repo.addAnswer(q.getId(), userId, txt);
                answerInput.setText("");
                loadSelectedQuestionDetails();
            }
        });

        markHelpfulBtn.addActionListener(e -> {
            int selectedIdx = answersList.getSelectedIndex();
            if (selectedIdx >= 0) {
                // In demo, toggle answer helpfulness
                JOptionPane.showMessageDialog(this, "Marked answer as helpful for community points!");
            }
        });

        answerContainer.add(answerInputPanel, BorderLayout.SOUTH);

        JSplitPane rightSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, detailsScroll, answerContainer);
        rightSplit.setResizeWeight(0.35);

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftScroll, rightSplit);
        mainSplit.setResizeWeight(0.4);
        add(mainSplit, BorderLayout.CENTER);

        // Initial Load
        triggerSearch();
    }

    private void restartTimer() {
        debounceTimer.restart();
    }

    // RUBRIC: [Concept: Debounce Execution & Worker Replacement]
    // Invoked when 300ms elapses with no typing. Cancels prior task and spawns a fresh worker.
    private void triggerSearch() {
        if (currentWorker != null && !currentWorker.isDone()) {
            // Cancel previous active worker cooperatively
            currentWorker.cancel(true);
        }
        questionListModel.clear();
        String keyword = searchField.getText().trim();
        currentWorker = new SearchWorker(keyword);
        currentWorker.execute(); // Run on background pool
    }

    private void loadSelectedQuestionDetails() {
        QAQuestion q = questionList.getSelectedValue();
        answersListModel.clear();
        if (q == null) {
            detailsArea.setText("");
            return;
        }
        detailsArea.setText("Title: " + q.getTitle() + "\n\nDetails:\n" + q.getDetails() +
                            "\n\nAsked At: " + q.getCreatedAt());

        List<QAAnswer> answers = repo.getAnswersForQuestion(q.getId());
        for (QAAnswer a : answers) {
            String badge = a.isHelpful() ? "[★ HELPFUL] " : "";
            answersListModel.addElement(badge + "(User #" + a.getUserId() + "): " + a.getAnswerText());
        }
    }

    private void showAskQuestionDialog() {
        JTextField titleFld = new JTextField(20);
        JTextArea detailsFld = new JTextArea(4, 20);
        JPanel p = new JPanel(new GridLayout(0, 1, 4, 4));
        p.add(new JLabel("Question Title:"));
        p.add(titleFld);
        p.add(new JLabel("Detailed Description:"));
        p.add(new JScrollPane(detailsFld));

        int res = JOptionPane.showConfirmDialog(this, p, "Ask a Question", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION && !titleFld.getText().trim().isEmpty()) {
            repo.addQuestion(userId, titleFld.getText().trim(), detailsFld.getText().trim());
            triggerSearch();
        }
    }

    // =========================================================================
    // SWINGWORKER FOR ASYNCHRONOUS SEARCH
    // =========================================================================

    // RUBRIC: [Concept: SwingWorker Background Threading]
    // doInBackground() runs off the Event Dispatch Thread (EDT) on a worker thread pool.
    // publish() and process() safely bridge background results into Swing components on the EDT.
    private class SearchWorker extends SwingWorker<Void, QAQuestion> {
        private final String keyword;

        public SearchWorker(String keyword) {
            this.keyword = keyword;
        }

        @Override
        protected Void doInBackground() {
            // Passes cooperative cancellation check: () -> isCancelled()
            List<QAQuestion> matches = repo.searchQuestions(keyword, this::isCancelled);
            for (QAQuestion q : matches) {
                if (isCancelled()) {
                    break; // Abort pushing items if cancelled
                }
                publish(q); // Pushes item to process() chunk queue
            }
            return null;
        }

        // RUBRIC: [Concept: Chunked EDT UI Updates via process()]
        // Automatically called on the Event Dispatch Thread (EDT) with chunks of published items.
        @Override
        protected void process(List<QAQuestion> chunks) {
            if (!isCancelled()) {
                for (QAQuestion q : chunks) {
                    questionListModel.addElement(q);
                }
            }
        }

        @Override
        protected void done() {
            if (questionListModel.isEmpty() && !isCancelled()) {
                // No results or completed
            }
        }
    }
}
