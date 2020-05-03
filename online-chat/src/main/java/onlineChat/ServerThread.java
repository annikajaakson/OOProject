package onlineChat;

import com.fasterxml.jackson.databind.ObjectMapper;
import components.chat.User;
import components.database.Database;
import components.request.*;
import de.mkammerer.argon2.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Path;
import java.util.List;

public class ServerThread implements Runnable {

    private final Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private List<Socket> clients;
    private List<User> users;
    private Path DBPath;
    private Database database;
    private ObjectMapper mapper = new ObjectMapper();
    private final Argon2 argon2 = Argon2Factory.create();

    public ServerThread(Socket socket, DataInputStream in, DataOutputStream out, List<User> users, List<Socket> clients, Database database, Path DBPath) {
        this.socket = socket;
        this.in = in;
        this.out = out;
        this.users = users;
        this.clients = clients;
        this.database = database;
        this.DBPath = DBPath;
    }

    public synchronized Database fetchDatabase () throws IOException {
        return mapper.readValue(new File(DBPath.toString()), Database.class);
    }

    public synchronized void writeDatabase () throws IOException {
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(DBPath.toString()), database);
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

                        User connectedUser = database.getUserByName(username);

                        // TODO: Figure out a better way to send user data from server
                        // Read user data from database
                        Database userData = database.getUserData(connectedUser);

                        // Send request response and user data to socket
                        out.writeUTF(mapper.writeValueAsString(new Response(ResponseType.OK_WITH_DATA, "User data on the way")));
                        out.writeUTF(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(userData));

                        // Add client to list of connected users
                        clients.add(socket);
                        users.add(connectedUser);
                        // Start new thread for processing incoming messages from that client
                        new Thread(new UserThread(socket, in, out, users, clients, database, DBPath)).start();

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

                        for (User user : database.getUsers()) {
                            // If user exists, check password
                            if (user.getUsername().equals(username)) {
                                connectedUser = user;

                                // If password is correct
                                if (argon2.verify(connectedUser.getPassword(), password.toCharArray())) {
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
                            newUser.setId(database.nextUserId());
                            newUser.setUsername(username);
                            newUser.setPassword(argon2.hash(30, 65536, 1, registerRequest.getPassword().toCharArray()));
                            newUser.setEmail(registerRequest.getEmail());

                            // Add user to database
                            database.getUsers().add(newUser);
                            writeDatabase();

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
            if (request.getRequestType() != RequestType.GETDATA) this.socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
