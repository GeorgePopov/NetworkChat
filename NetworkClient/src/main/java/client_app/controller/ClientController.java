package client_app.controller;

import client_app.model.NetworkService;
import client_app.view.AuthDialog;
import client_app.view.ClientChat;
import client_server_app.Command;

import javax.swing.*;
import java.io.IOException;
import java.util.List;

public class ClientController {
    private final NetworkService networkService;
    private final AuthDialog authDialog;
    private final ClientChat clientChat;
    private String nickname;
    public static final String ALL_USERS_LIST_ITEM = "All";

    public ClientController(String serverHost, int serverPort) {
        this.networkService = new NetworkService(serverHost, serverPort, this);
        this.authDialog = new AuthDialog(this);
        this.clientChat = new ClientChat(this);
    }

    public String getUsername() {
        return nickname;
    }

    private void setUsername(String nickname) {
        SwingUtilities.invokeLater(() -> clientChat.setTitle(nickname));
        this.nickname = nickname;
    }

    public void runApplication() throws IOException {
        connectedToServer();
        runAuthProcess();
    }

    private void connectedToServer() throws IOException {
        try {
            networkService.connect();
        } catch (Exception e) {
            System.err.println("Failed to establish network chat server connection");
            throw e;
        }
    }

    private void runAuthProcess() {
        networkService.setSuccessfulAuthEvent(nickname -> {
            ClientController.this.setUsername(nickname);
            ClientController.this.openChat();
        });
        authDialog.setVisible(true);
    }

    private void openChat() {
        authDialog.dispose();
        networkService.setMessageHandler(clientChat::appendMessage);
        clientChat.setVisible(true);
    }

    public void sendCommand(Command command) {
        try {
            networkService.sendCommand(command);
        } catch (IOException e) {
            showErrorMessage(e.getMessage());
        }
    }

    public void showErrorMessage(String errorMessage) {
        if (clientChat.isActive()) {
            clientChat.showError(errorMessage);
        } else if (authDialog.isActive()) {
            authDialog.showError(errorMessage);
        }
        System.err.println(errorMessage);
    }

    public void sendAuthMessage(String login, String password) {
        sendCommand(Command.authCommand(login, password));
    }

    public void sendMessage(String message) {
        sendCommand(Command.broadcastMessageCommand(message));
    }

    public void sendPrivateMessage(String username, String message) {
        sendCommand(Command.privateMessageCommand(username, message));
    }

    public void updateUsersList(List<String> users) {
        users.remove(nickname);
        users.add(0, ALL_USERS_LIST_ITEM);
        clientChat.updateUsers(users);
    }

    public void shutdown() {
        networkService.close();
    }
}