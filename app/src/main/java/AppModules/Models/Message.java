package AppModules.Models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Message {
    public static final int TYPE_USER = 1;
    public static final int TYPE_BOT = 2;

    private String text;
    private int type;
    private long timestamp;

    public Message(String text, int type) {
        this.text = text;
        this.type = type;
        this.timestamp = System.currentTimeMillis();
    }

    public String getText() {
        return text;
    }

    public int getType() {
        return type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getFormattedTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }
}

