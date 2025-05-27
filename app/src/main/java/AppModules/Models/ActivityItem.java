package AppModules.Models;
public class ActivityItem {
    private String message;
    private String referenceId;
    private String type;

    public ActivityItem(String message, String referenceId, String type) {
        this.message = message;
        this.referenceId = referenceId;
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public String getType() {
        return type;
    }
}
