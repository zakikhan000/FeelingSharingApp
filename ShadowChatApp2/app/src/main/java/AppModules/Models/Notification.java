package AppModules.Models;

public class Notification {
    private int notificationId;
    private int uaid;
    private int senderUaid;
    private String type;
    private int referenceId;
    private String message;
    private boolean isRead;
    private String createdAt;

    // Constructor
    public Notification(int notificationId, int uaid, int senderUaid, String type,
                        int referenceId, String message, boolean isRead, String createdAt) {
        this.notificationId = notificationId;
        this.uaid = uaid;
        this.senderUaid = senderUaid;
        this.type = type;
        this.referenceId = referenceId;
        this.message = message;
        this.isRead = isRead;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public int getNotificationId() { return notificationId; }
    public void setNotificationId(int notificationId) { this.notificationId = notificationId; }

    public int getUaid() { return uaid; }
    public void setUaid(int uaid) { this.uaid = uaid; }

    public int getSenderUaid() { return senderUaid; }
    public void setSenderUaid(int senderUaid) { this.senderUaid = senderUaid; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getReferenceId() { return referenceId; }
    public void setReferenceId(int referenceId) { this.referenceId = referenceId; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}