package AppModules.Models;

public class PublicPost {
    private int pid;
    private String title;
    private String content;
    private String createdAt;
    private int uaid;
    private String userName;
    private String userProfileImage;

    // Default constructor


    // Getters and Setters
    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public int getUaid() {
        return uaid;
    }

    public void setUaid(int uaid) {
        this.uaid = uaid;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserProfileImage() {
        return userProfileImage;
    }

    public void setUserProfileImage(String userProfileImage) {
        this.userProfileImage = userProfileImage;
    }

    // Helper method to format the date
    public String getFormattedDate() {
        if (createdAt == null || createdAt.isEmpty()) {
            return "";
        }
        // You can add date formatting logic here if needed
        return createdAt;
    }
}