package components.database;

import components.chat.Conversation;
import components.chat.User;

import java.util.ArrayList;
import java.util.List;

public class Database {
    private List<User> users;

    private List<Conversation> conversations;

    public Database () {}

    public Database(Database copy) {
        this.users = new ArrayList<>(copy.getUsers());
        this.conversations = new ArrayList<>(copy.getConversations());
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }

    public List<Conversation> getConversations() {
        return conversations;
    }

    public void setConversations(List<Conversation> conversations) {
        this.conversations = conversations;
    }
}
