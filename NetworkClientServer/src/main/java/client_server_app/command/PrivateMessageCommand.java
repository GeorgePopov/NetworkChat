package client_server_app.command;

import java.io.Serializable;

public class PrivateMessageCommand implements Serializable {
    private final String receiver;
    private final String message;

    public PrivateMessageCommand(String receiver, String message) {
        this.receiver = receiver;
        this.message = message;
    }

    public String getReceiver() {
        return receiver;
    }

    public String getMessage() {
        return message;
    }
}
