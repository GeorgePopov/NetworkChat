package client_app;

import client_app.controller.ClientController;

public class NetworkClient {
    public static void main(String[] args) {
        try {
            ClientController controller = new ClientController("localhost", 8888);
            controller.runApplication();
        } catch (Exception e) {
            System.err.println("Failed to connect to network chat server. Pleas, check you network setting ");
            e.printStackTrace();
        }
    }
}