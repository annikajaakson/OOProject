package components.database;

import components.chat.User;

import java.util.List;

public class Database {
    List<User> users;

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }
}
