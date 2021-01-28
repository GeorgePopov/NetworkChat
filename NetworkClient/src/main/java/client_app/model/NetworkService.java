package client_app.model;

import client_app.controller.AuthEvent;
import client_app.controller.ClientController;
import client_server_app.Command;
import client_server_app.command.AuthCommand;
import client_server_app.command.ErrorCommand;
import client_server_app.command.MessageCommand;
import client_server_app.command.UpdateUsersListCommand;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.function.Consumer;

public class NetworkService {
    private final String host;
    private final int port;
    private final ClientController controller;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Consumer<String> messageHandler;
    private AuthEvent successfulAuthEvent;

    public NetworkService(String host, int port, ClientController controller) {
        this.host = host;
        this.port = port;
        this.controller = controller;
    }

    public void setMessageHandler(Consumer<String> messageHandler) {
        this.messageHandler = messageHandler;
    }

    public void setSuccessfulAuthEvent(AuthEvent successfulAuthEvent) {
        this.successfulAuthEvent = successfulAuthEvent;
    }

    public void connect() throws IOException {
        socket = new Socket(host, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        in = new ObjectInputStream(socket.getInputStream());
        runReadThread();
    }

    private void runReadThread() {
        new Thread(() -> {
            while (true) {
                try {
                    Command command = (Command) in.readObject();
                    processCommand(command);
                } catch (IOException e) {
                    System.err.println("The read stream was interrupted");
                    return;
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void processCommand(Command command) {
        switch (command.getType()) {
            case AUTH: {
                processAuthCommand(command);
                break;
            }
            case MESSAGE: {
                processMessageCommand(command);
                break;
            }
            case AUTH_ERROR:
            case ERROR: {
                processErrorCommand(command);
                break;
            }
            case UPDATE_USERS_LIST: {
                UpdateUsersListCommand data = (UpdateUsersListCommand) command.getData();
                List<String> users = data.getUsers();
                controller.updateUsersList(users);
                break;
            }
            default:
                System.err.println("Unknown type of command: " + command.getType());
        }
    }

    private void processAuthCommand(Command command) {
        AuthCommand data = (AuthCommand) command.getData();
        String nickname = data.getUsername();
        successfulAuthEvent.authIsSuccessful(nickname);
    }

    private void processMessageCommand(Command command) {
        MessageCommand data = (MessageCommand) command.getData();
        if (messageHandler != null) {
            String message = data.getMessage();
            String username = data.getUsername();
            if (username != null) {
                message = username + ": " + message;
            }
            messageHandler.accept(message);
        }
    }

    private void processErrorCommand(Command command) {
        ErrorCommand data = (ErrorCommand) command.getData();
        controller.showErrorMessage(data.getErrorMessage());
    }

    public void sendCommand(Command command) throws IOException {
        out.writeObject(command);
    }

    public void close() {
        try {
            sendCommand(Command.endCommand());
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}