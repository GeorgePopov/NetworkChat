package client_server_app;

public enum CommandType {
    AUTH,
    AUTH_ERROR,
    MESSAGE,
    BROADCAST_MESSAGE,
    PRIVATE_MESSAGE,
    UPDATE_USERS_LIST,
    ERROR,
    END
}
