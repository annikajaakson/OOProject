package components.database;

import components.chat.Conversation;
import components.chat.Message;
import components.chat.User;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    public User getUserById(int id) {
        return users.stream().filter(user -> user.getId() == id).findFirst().orElse(null);
    }

    public User getUserByName(String username) {
        return users.stream()
                .filter(user -> user.getUsername().equals(username))
                .findFirst()
                .orElse(null);
    }

    public Conversation getConversationById(int id) {
        return conversations.stream()
                .filter(convo -> convo.getId() == id)
                .findFirst()
                .orElse(null);
    }

    public Message getMessageById(int convoId, int messageId) {
        return getConversationById(convoId).getMessages().stream()
                .filter(message -> message.getId() == messageId)
                .findFirst()
                .orElse(null);
    }

    public int nextUserId() {
        int maxId = users.stream().mapToInt(User::getId).max().orElse(0);

        if (maxId == users.size()) {
            return users.size() + 1;
        }

        for (int possibleId = 1; possibleId < maxId; possibleId++) {
            // Temp variable because lambda only allows effectively final variables
            int finalPossibleId = possibleId;
            if (users.stream().mapToInt(User::getId).noneMatch(id -> id == finalPossibleId)) {
                return possibleId;
            }
        }

        // Should never reach
        return 0;
    }

    public int nextConversationId() {
        int maxId = conversations.stream().mapToInt(Conversation::getId).max().orElse(0);

        if (maxId == conversations.size()) {
            return conversations.size() + 1;
        }

        for (int possibleId = 1; possibleId < maxId; possibleId++) {
            // Temp variable because lambda only allows effectively final variables
            int finalPossibleId = possibleId;
            if (conversations.stream().mapToInt(Conversation::getId).noneMatch(id -> id == finalPossibleId)) {
                return possibleId;
            }
        }

        // Should never reach
        return 0;
    }

    public int nextMessageId(int convoId) {
        List<Message> convoMessages = getConversationById(convoId).getMessages();

        int maxId = convoMessages.stream().mapToInt(Message::getId).max().orElse(0);

        if (maxId == convoMessages.size()) {
            return convoMessages.size() + 1;
        }

        for (int possibleId = 1; possibleId < maxId; possibleId++) {
            // Temp variable because lambda only allows effectively final variables
            int finalPossibleId = possibleId;
            if (convoMessages.stream().mapToInt(Message::getId).noneMatch(id -> id == finalPossibleId)) {
                return possibleId;
            }
        }

        // Should never reach
        return 0;
    }

    // Get subdatabase for information displayed to one user
    public Database getUserData(User user) {
        if (getUserById(user.getId()) == null) return null;

        // Copy existing database
        Database userData = new Database(this);

        // Remove all conversations that don't include the user
        userData.setConversations(userData.getConversations().stream()
                .filter(conversation -> conversation.getParticipants().contains(user))
                .collect(Collectors.toList()));

        // Remove all users who are not in the given user's contacts list or participants in the given user's conversations
        userData.setUsers(userData.getUsers().stream()
                .filter(existingUser -> user.getContacts().contains(existingUser) || userData.getConversations().stream()
                        .anyMatch(conversation -> conversation.getParticipants().contains(existingUser) && existingUser != user))
                .map(existingUser -> new User(existingUser)) // Create copies of users so original database won't be affected by user personal data removal
                .collect(Collectors.toList()));

        // Remove all delicate personal data from other users
        userData.getUsers().forEach(existingUser -> {
            if (existingUser != user) {
                existingUser.setEmail(null);
                existingUser.setPassword(null);
                existingUser.setConversations(new ArrayList<>());
                existingUser.setContacts(new ArrayList<>());
            }
        });

        // Add given user copy as first element of users list
        userData.getUsers().add(0, new User(user));

        // Replace conversations with their copies so original database stays intact
        userData.setConversations(userData.getConversations().stream()
                .map(existingConvo -> {
                    var newConvo = new Conversation(existingConvo);
                    // Make conversation participants refer to their copies in userData
                    newConvo.setParticipants(newConvo.getParticipants().stream()
                            .map(participant -> userData.getUserById(participant.getId()))
                            .collect(Collectors.toList()));
                    return newConvo;
                })
                .collect(Collectors.toList()));

        return userData;
    }

    // Get subdatabase for information related to one conversation
    public Database getConversationData(Conversation conversation) {
        if (getConversationById(conversation.getId()) == null) return null;

        // Copy existing database
        Database conversationData = new Database(this);

        conversationData.setConversations(List.of(new Conversation(conversation)));

        // Remove all users that are not part of this conversation
        conversationData.setUsers(conversationData.getUsers().stream()
                .filter(user -> conversation.getParticipants().contains(user))
                .map(existingUser -> new User(existingUser)) // Create copies of users so original database won't be affected by user personal data removal
                .collect(Collectors.toList()));


        // Remove all delicate personal data from users
        conversationData.getUsers().forEach(existingUser -> {
            existingUser.setEmail(null);
            existingUser.setPassword(null);
            existingUser.setConversations(new ArrayList<>());
            existingUser.setContacts(new ArrayList<>());
        });

        // Replace conversations with their copies so original database stays intact
        conversationData.setConversations(conversationData.getConversations().stream()
                .map(existingConvo -> {
                    var newConvo = new Conversation(existingConvo);
                    // Make conversation participants refer to their copies in userData
                    newConvo.setParticipants(newConvo.getParticipants().stream()
                            .map(participant -> conversationData.getUserById(participant.getId()))
                            .collect(Collectors.toList()));
                    return newConvo;
                })
                .collect(Collectors.toList()));

        return conversationData;
    }
}
