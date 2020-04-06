package onlineChat;

import components.chat.Conversation;
import components.chat.Message;
import components.database.Database;
import components.chat.User;

import com.fasterxml.jackson.databind.ObjectMapper;
import components.request.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class socketThread implements Runnable {

    private final Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private Path DBPath;
    private Database database;
    private ObjectMapper mapper;

    public socketThread(Socket socket, DataInputStream in, DataOutputStream out, Path DBPath) {
        this.socket = socket;
        this.in = in;
        this.out = out;
        this.DBPath = DBPath;
        this.mapper = new ObjectMapper();
    }

    public Database fetchDatabase () throws IOException {
        return mapper.readValue(new File(DBPath.toString()), Database.class);
    }

    public Database fetchUserData(String username) {
        // TODO: Figure out a better way to send user data from server

        Database userData = new Database(database);
        Set<User> users = new HashSet<>();

        // Current user
        User fetchedUser = userData.getUsers().stream()
                .filter(user -> user.getUsername().equals(username))
                .findFirst()
                .get();

        // Add requested user's contacts to database user list
        users.addAll(userData.getUsers().stream()
                .filter(user -> fetchedUser.getContacts().stream().anyMatch(contact -> contact.getId() == user.getId()))
                .collect(Collectors.toList()));

        // Remove all conversations that don't include the current user
        // And add all participants from the current user's convos to users list
        for (int c = userData.getConversations().size() - 1; c >= 0; c--) {
            Conversation convo = userData.getConversations().get(c);
            if (convo.getParticipants().stream()
                    .noneMatch(user -> user.getId() == fetchedUser.getId())) {
                userData.getConversations().remove(c);
            } else {
                users.addAll(convo.getParticipants());
            }
        }

        userData.setUsers(new ArrayList<>(users));

        // Remove fetched user from users list
        userData.getUsers().remove(userData.getUsers().stream()
                .filter(user -> user.getId() == fetchedUser.getId())
                .findFirst()
                .get());

        // Add current user as first element in users list
        userData.getUsers().add(0, fetchedUser);

        // Remove unnecessary info from other users visible to the current one, such as password and their own contacts
        for (int i = 1; i < userData.getUsers().size(); i++) {
            User contactCopy = new User();

            contactCopy.setId(userData.getUsers().get(i).getId());
            contactCopy.setUsername(userData.getUsers().get(i).getUsername());
            contactCopy.setEmail(userData.getUsers().get(i).getEmail());

            userData.getUsers().set(i, contactCopy);
        }

        for (Conversation convo : userData.getConversations()) {
            // Update message senders
            for (Message m : convo.getMessages()) {
                m.setSender(userData.getUsers().stream()
                        .filter(user -> user.getId() == m.getSender().getId())
                        .findFirst()
                        .get());
            }
        }

        return userData;
    }

    public void run() {
        try {
            String objectAsString = in.readUTF();

            // Read the string as a general ClientRequest first
            var request = mapper.readValue(objectAsString, ClientRequest.class);

            switch (request.getRequestType()) {
                case GETDATA:
                    try {
                        // Now that we know the request type, read string as the right type
                        var getDataRequest = mapper.readValue(objectAsString, GetDataRequest.class);

                        // Read who connected
                        String username = getDataRequest.getUsername();
                        System.out.println("user " + username + " connected");

                        // Refresh database
                        database = fetchDatabase();

                        // TODO: Figure out a better way to send user data from server
                        // Read user data from database
                        Database userData = fetchUserData(username);

                        // Send request response and user data to socket
                        out.writeUTF(mapper.writeValueAsString(new Response(ResponseType.OK_WITH_DATA, "User data on the way")));
                        out.writeUTF(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(userData));

                        System.out.println("user information presented for " + username);
                        System.out.println();

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                // Login event occured
                case LOGIN:
                    try {
                        // Now that we know the request type, read string as the right type
                        var loginRequest = mapper.readValue(objectAsString, LoginRequest.class);

                        // Read who connected
                        String username = loginRequest.getUsername();
                        System.out.println("Login attempt with username " + username);

                        String password = loginRequest.getPassword();

                        User connectedUser = null;

                        // Refresh database
                        database = fetchDatabase();

                        for (User user : database.getUsers()) {
                            // If user exists, check password
                            if (user.getUsername().equals(username)) {
                                connectedUser = user;

                                // If password is correct
                                if (password.equals(user.getPassword())) {
                                    // Send response to client
                                    out.writeUTF(mapper.writeValueAsString(new Response(ResponseType.OK_NO_DATA, "Login completed successfully")));
                                } else {
                                    // Send response (error) to client
                                    out.writeUTF(mapper.writeValueAsString(new Response(ResponseType.ERROR, "Wrong password")));
                                }
                                break;
                            }
                        }

                        if (connectedUser == null) {
                            // Send response (error) to client
                            out.writeUTF(mapper.writeValueAsString(new Response(ResponseType.ERROR, "Unknown user")));
                        }

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                // Register event occured
                case REGISTER:
                    try {
                        // Now that we know the request type, read string as the right type
                        var registerRequest = mapper.readValue(objectAsString, RegisterRequest.class);

                        // Read who is trying to register
                        String username = registerRequest.getUsername();
                        System.out.println("Register request with username " + username);

                        // Refresh database
                        database = fetchDatabase();

                        // Check if username is taken
                        boolean userNameTaken = false;
                        for (User user : database.getUsers()) {
                            if (user.getUsername().equals(username)) {
                                // Send response (error) to client
                                out.writeUTF(mapper.writeValueAsString(new Response(ResponseType.ERROR, "Username already in use")));
                                userNameTaken = true;
                                break;
                            }
                        }

                        // If username is not taken, create a new user
                        if (!userNameTaken) {
                            User newUser = new User();
                            // TODO: Better ID generation required
                            newUser.setId(database.getUsers().size() + 1);
                            newUser.setUsername(username);
                            newUser.setPassword(registerRequest.getPassword());
                            newUser.setEmail(registerRequest.getEmail());

                            // Add user to database
                            database.getUsers().add(newUser);
                            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(DBPath.toString()), database);

                            // Send response to client
                            out.writeUTF(mapper.writeValueAsString(new Response(ResponseType.OK_NO_DATA, "New user registered")));
                        }

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                default:
                    try {
                        throw new IllegalAccessException("Illegal prefix");
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
            }

            // Close the socket manually
            this.socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

public class Server {
    private static String databasePath = "db-files/database.json";

    public static void main(String[] args) throws IOException {
        Path DBPath = Paths.get(databasePath);

        int port = 1337;

        try (ServerSocket ss = new ServerSocket(port)) {
            while (true) {
                try {
                    Socket socket = ss.accept();
                    DataInputStream in = new DataInputStream(socket.getInputStream());
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());

                    Thread thread = new Thread(new socketThread(socket, in, out, DBPath));
                    thread.start();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}