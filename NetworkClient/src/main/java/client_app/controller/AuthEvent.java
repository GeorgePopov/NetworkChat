package client_app.controller;

@FunctionalInterface
public interface AuthEvent {
    void authIsSuccessful(String nickname);
}