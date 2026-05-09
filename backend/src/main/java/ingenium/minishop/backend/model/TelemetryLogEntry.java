package ingenium.minishop.backend.model;

public class TelemetryLogEntry {
    private String timestamp;
    private String craftId;
    private String craftName;
    private String eventType;
    private String message;
    private SpacecraftStatus snapshot;

    public TelemetryLogEntry() {
    }

    public TelemetryLogEntry(
            String timestamp,
            String craftId,
            String craftName,
            String eventType,
            String message,
            SpacecraftStatus snapshot
    ) {
        this.timestamp = timestamp;
        this.craftId = craftId;
        this.craftName = craftName;
        this.eventType = eventType;
        this.message = message;
        this.snapshot = snapshot;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public String getCraftId() {
        return craftId;
    }

    public void setCraftId(String craftId) {
        this.craftId = craftId;
    }

    public String getCraftName() {
        return craftName;
    }

    public void setCraftName(String craftName) {
        this.craftName = craftName;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public SpacecraftStatus getSnapshot() {
        return snapshot;
    }

    public void setSnapshot(SpacecraftStatus snapshot) {
        this.snapshot = snapshot;
    }
}

