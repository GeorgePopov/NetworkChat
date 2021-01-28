package server_app.network_server.client_handler;

import client_server_app.Command;
import client_server_app.command.AuthCommand;
import client_server_app.command.BroadcastMessageCommand;
import client_server_app.command.PrivateMessageCommand;
import server_app.network_server.MyServer;
import server_app.network_server.auth.AuthService;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler {
    private final MyServer serverInstance;
    private final Socket clientSocket;
    private final AuthService authService;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    private String nickname;

    public ClientHandler(Socket clientSocket, MyServer myServer) {
        this.clientSocket = clientSocket;
        serverInstance = myServer;
        this.authService = myServer.getAuthService();
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public void handle() throws IOException {
        inputStream = new ObjectInputStream(clientSocket.getInputStream());
        outputStream = new ObjectOutputStream(clientSocket.getOutputStream());

        new Thread(() -> {
            try {
                authentication();
                readMessage();
            } catch (IOException e) {
                System.err.println("Connection has been failed");
            } finally {
                closeConnection();
            }
        }).start();
    }

    private void authentication() throws IOException {
        while (true) {
            Command command = readCommand();
            if (command == null) {
                continue;
            }
            switch (command.getType()) {
                case AUTH: {
                    if (processAuthCommand(command)) {
                        return;
                    }
                    break;
                }
                default: {
                    String errorMessage = "Illegal command for authentication: " + command.getType();
                    reportBag(errorMessage);
                }
            }
        }
    }

    private boolean processAuthCommand(Command command) throws IOException {
        AuthCommand authCommand = (AuthCommand) command.getData();
        String login = authCommand.getLogin();
        String password = authCommand.getPassword();

        String nickname = authService.getNicknameByLoginAndPassword(login, password);
        if (nickname == null) {
            sendMessage(Command.authErrorCommand("Incorrect login/password"));
        } else if (serverInstance.isNicknameBusy(nickname)) {
            sendMessage(Command.authErrorCommand("The account is already in use"));
        } else {
            authCommand.setUsername(nickname);
            sendMessage(command);
            setNickname(nickname);
            serverInstance.broadcastMessage(Command.messageCommand(null, nickname + " entered the chat"));
            serverInstance.subscribe(this);
            return true;
        }
        return false;
    }

    private void readMessage() throws IOException {
        while (true) {
            Command command = readCommand();
            if (command == null) {
                continue;
            }
            switch (command.getType()) {
                case END:
                    return;
                case BROADCAST_MESSAGE:
                    BroadcastMessageCommand broadcastMessageCommand = (BroadcastMessageCommand) command.getData();
                    serverInstance.broadcastMessage(Command.messageCommand(nickname, broadcastMessageCommand.getMessage()));
                    break;
                case PRIVATE_MESSAGE:
                    PrivateMessageCommand privateMessageCommand = (PrivateMessageCommand)  command.getData();
                    String receiver = privateMessageCommand.getReceiver();
                    String message = privateMessageCommand.getMessage();
                    serverInstance.sendPrivateMessage(receiver, Command.messageCommand(nickname, message));
                    break;
                default:
                    String errorMessage = "Unknown type of command: " + command.getType();
                    reportBag(errorMessage);
            }
        }
    }

    private Command readCommand() throws IOException{
        try {
            return (Command) inputStream.readObject();
        } catch (ClassNotFoundException e) {
            String errorMessage = "Unknown type of object from network chat client";
            reportBag(errorMessage);
            return null;
        }
    }

    public void sendMessage(Command command) throws IOException {
        outputStream.writeObject(command);
    }

    private void closeConnection() {
        try {
            serverInstance.unsubscribe(this);
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void reportBag(String errorMessage) throws IOException {
        System.out.println(errorMessage);
        sendMessage(Command.errorCommand(errorMessage));
    }
}