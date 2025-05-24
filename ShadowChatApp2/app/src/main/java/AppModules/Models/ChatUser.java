package AppModules.Models;

public class ChatUser {
    private String id; // uaid
    private String name; // username
    private String profileImage;
    private String lastMessage;
    private String lastAction;
    private int unseenCount;
    private String lastMessageTime;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getProfileImage() { return profileImage; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public String getLastAction() { return lastAction; }
    public void setLastAction(String lastAction) { this.lastAction = lastAction; }

    public int getUnseenCount() { return unseenCount; }
    public void setUnseenCount(int unseenCount) { this.unseenCount = unseenCount; }

    public String getLastMessageTime() { return lastMessageTime; }
    public void setLastMessageTime(String lastMessageTime) { this.lastMessageTime = lastMessageTime; }
}