public class QAAnswer {
    private int id;
    private int questionId;
    private int userId;
    private String answerText;
    private boolean isHelpful;

    public QAAnswer() {}

    public QAAnswer(int id, int questionId, int userId, String answerText, boolean isHelpful) {
        this.id = id;
        this.questionId = questionId;
        this.userId = userId;
        this.answerText = answerText;
        this.isHelpful = isHelpful;
    }

    public QAAnswer(int questionId, int userId, String answerText) {
        this(0, questionId, userId, answerText, false);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getQuestionId() { return questionId; }
    public void setQuestionId(int questionId) { this.questionId = questionId; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getAnswerText() { return answerText; }
    public void setAnswerText(String answerText) { this.answerText = answerText; }

    public boolean isHelpful() { return isHelpful; }
    public void setHelpful(boolean helpful) { isHelpful = helpful; }
}
