package server_app;

import server_app.network_server.MyServer;

public class ServerApp {
    private static final int PORT = 8888;

    public static void main(String[] args) {
        MyServer myServer = new MyServer(PORT);
        myServer.start();
    }
}
