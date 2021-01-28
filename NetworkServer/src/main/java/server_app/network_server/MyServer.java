package server_app.network_server;

import server_app.network_server.auth.AuthService;
import server_app.network_server.auth.BaseAuthService;
import server_app.network_server.client_handler.ClientHandler;
import client_server_app.Command;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MyServer {
    private final int port;
    private final List<ClientHandler> clients;
    private final AuthService authService;

    public MyServer(int port) {
        this.port = port;
        clients = new ArrayList<>();
        authService = new BaseAuthService();
    }

    public AuthService getAuthService() {
        return authService;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is running");
            authService.start();

            //noinspection InfiniteLoopStatement
            while (true) {
                System.out.println("Waiting for network chat client connection...");
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client has been connected");
                ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                try {
                    clientHandler.handle();
                } catch (IOException e) {
                    System.err.println("Failed to handle network chat client connection");
                    clientSocket.close();
                }
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        } finally {
            authService.stop();
        }
    }

    public boolean isNicknameBusy(String nickname) {
        for (ClientHandler client : clients) {
            if (client.getNickname().equals(nickname)) {
                return true;
            }
        }
        return false;
    }

    public synchronized void subscribe(ClientHandler clientHandler) throws IOException {
        clients.add(clientHandler);
        List<String> users = getAllUsername();
        broadcastMessage(Command.updateUsersListCommand(users));
    }

    public synchronized void unsubscribe(ClientHandler clientHandler) throws IOException {
        clients.remove(clientHandler);
        List<String> users = getAllUsername();
        broadcastMessage(Command.updateUsersListCommand(users));
    }

    private List<String> getAllUsername() {
        /*return clients.stream()*/
//                .map(ClientHandler::getNickname)
//                .collect(Collectors.toList());
        List<String> result = new ArrayList<>();
        for (ClientHandler client : clients) {
            result.add(client.getNickname());
        }
        return result;
    }

    public void broadcastMessage(Command command) throws IOException {
        for (ClientHandler client : clients) {
            client.sendMessage(command);
        }
    }

    public synchronized void sendPrivateMessage(String receiver, Command command) throws IOException {
        for (ClientHandler client : clients) {
            if (client.getNickname().equals(receiver)) {
                client.sendMessage(command);
            }
        }
    }
}