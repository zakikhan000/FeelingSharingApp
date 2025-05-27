package AppModules.Models;
import java.io.Serializable;
public class Comment  implements Serializable {
    private int commentId;
    private int pid;
    private int uaid;
    private String commentText;
    private int sId;
    private double sentimentScore;
    private String createdAt;
    private String userName;
    private String profileImage;
    private String nameVisibility;
    private String profileVisibility;

    public Comment() {
        // Default constructor
    }

    // Getters and Setters
    public int getCommentId() {
        return commentId;
    }

    public void setCommentId(int commentId) {
        this.commentId = commentId;
    }

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public int getUaid() {
        return uaid;
    }

    public void setUaid(int uaid) {
        this.uaid = uaid;
    }

    public String getCommentText() {
        return commentText;
    }

    public void setCommentText(String commentText) {
        this.commentText = commentText;
    }

    public int getSId() {
        return sId;
    }

    public void setSId(int sId) {
        this.sId = sId;
    }

    public double getSentimentScore() {
        return sentimentScore;
    }

    public void setSentimentScore(double sentimentScore) {
        this.sentimentScore = sentimentScore;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public String getNameVisibility() {
        return nameVisibility;
    }

    public void setNameVisibility(String nameVisibility) {
        this.nameVisibility = nameVisibility;
    }

    public String getProfileVisibility() {
        return profileVisibility;
    }

    public void setProfileVisibility(String profileVisibility) {
        this.profileVisibility = profileVisibility;
    }
}