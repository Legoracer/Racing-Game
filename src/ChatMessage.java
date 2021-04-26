import java.io.Serializable;

public class ChatMessage implements Serializable {
    String content = "";
    String client = "";

    /**
     * Basic constructor
     * @param content
     */
    public ChatMessage(String content) {
        if (content.startsWith("/w")) {
        }

        this.content = content;
    }
}