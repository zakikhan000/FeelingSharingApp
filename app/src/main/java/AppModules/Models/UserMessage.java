package AppModules.Models;

public class UserMessage {
    private String sender;
    private String receiver;
    private String message;
    private String datetime;
    private double sentiment;

    public UserMessage(String sender, String receiver, String message, String datetime) {
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
        this.datetime = datetime;
        this.sentiment = 0; // Default sentiment value
    }

    // Add getters and setters
    public String getSender() {
        return sender;
    }

    public String getReceiver() {
        return receiver;
    }

    public String getMessage() {
        return message;
    }

    public String getDatetime() {
        return datetime;
    }

    public double getSentiment() {
        return sentiment;
    }

    public void setSentiment(double sentiment) {
        this.sentiment = sentiment;
    }
}
