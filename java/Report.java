/** Member 5: a report for moderation (table: reports). Target can be a user or Member 4 content. */
public class Report {
    public static final String PENDING = "PENDING", RESOLVED = "RESOLVED", DISMISSED = "DISMISSED";
    public static final String USER = "USER", QA_ANSWER = "QA_ANSWER", DISCUSSION_POST = "DISCUSSION_POST";
    private final int id, reporterId, reportedId, targetId;
    private final String reporterName, reportedName, reason, status, note, createdAt, targetType;
    public Report(int id, int reporterId, String reporterName, int reportedId, String reportedName,
                  String targetType, int targetId, String reason, String status, String note, String createdAt) {
        this.id = id; this.reporterId = reporterId; this.reporterName = reporterName;
        this.reportedId = reportedId; this.reportedName = reportedName; this.targetType = targetType; this.targetId = targetId;
        this.reason = reason; this.status = status; this.note = note == null ? "" : note; this.createdAt = createdAt;
    }
    public int getId() { return id; }
    public int getReporterId() { return reporterId; }
    public String getReporterName() { return reporterName; }
    public int getReportedId() { return reportedId; }
    public String getReportedName() { return reportedName; }
    public String getTargetType() { return targetType; }
    public int getTargetId() { return targetId; }
    public String getReason() { return reason; }
    public String getStatus() { return status; }
    public String getNote() { return note; }
    public String getCreatedAt() { return createdAt; }
    public boolean isPending() { return PENDING.equals(status); }
    public boolean isContent() { return !USER.equals(targetType) && targetId > 0; }
    public String getTargetLabel() { return USER.equals(targetType) ? "User" : targetType + " #" + targetId; }
}
