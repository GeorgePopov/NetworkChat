package server_app.network_server.auth;

public interface AuthService {
    String getNicknameByLoginAndPassword(String login, String password);
    void start();
    void stop();
}
