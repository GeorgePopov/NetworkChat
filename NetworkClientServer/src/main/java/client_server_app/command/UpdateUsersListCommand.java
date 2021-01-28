package client_server_app.command;

import java.io.Serializable;
import java.util.List;

public class UpdateUsersListCommand implements Serializable {
    private final List<String> users;

    public UpdateUsersListCommand(List<String> users) {
        this.users = users;
    }

    public List<String> getUsers() {
        return users;
    }
}
