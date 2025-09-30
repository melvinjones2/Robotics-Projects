package client;

public class Message {

    private final String type;
    private final String payload;

    public Message(String type, String payload) {
        this.type = type;
        this.payload = payload;
    }

    public String getType() {
        return type;
    }

    public String getPayload() {
        return payload;
    }

    // Construct a message string
    public static String construct(String type, String payload) {
        return type + ":" + payload;
    }

    // Parse a message string into a Message object
    public static Message parse(String msg) {
        int idx = msg.indexOf(':');
        if (idx == -1) {
            return new Message(msg.trim(), "");
        }
        String type = msg.substring(0, idx).trim();
        String payload = msg.substring(idx + 1).trim();
        return new Message(type, payload);
    }
}
