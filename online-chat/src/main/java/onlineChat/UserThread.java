package onlineChat;

import com.fasterxml.jackson.databind.ObjectMapper;
import components.chat.Message;
import components.chat.User;
import components.database.Database;
import components.request.LogoutRequest;
import components.request.MessageRequest;
import components.request.Response;
import components.request.ResponseType;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Path;
import java.util.List;

public class UserThread implements Runnable {

    private final Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private List<User> users;
    private List<Socket> clients;
    private Path DBPath;
    private Database database;
    private ObjectMapper mapper = new ObjectMapper();

    public UserThread(Socket socket, DataInputStream in, DataOutputStream out, List<User> users, List<Socket> clients, Database database, Path DBPath) {
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
        boolean running = true;
        while (running) {
            try {
                String objectAsString = in.readUTF();
                // Read the string as a general ClientRequest first
                var request = mapper.readValue(objectAsString, ClientRequest.class);

                switch (request.getRequestType()) {
                    case LOGOUT:
                        try {
                            // Now that we know the request type, read string as the right type
                            var logoutRequest = mapper.readValue(objectAsString, LogoutRequest.class);

                            // Read who is trying to log out
                            String username = logoutRequest.getUsername();
                            System.out.println("Log out request with username " + username);

                            // Remove client from sockets and users lists
                            clients.remove(socket);
                            users.removeIf(user -> user.getUsername().equals(username));

                            // Close socket and thread
                            socket.close();
                            running = false;
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        break;
                    case MESSAGE:
                        var messageRequest = mapper.readValue(objectAsString, MessageRequest.class);
                        // Find conversation that the message belongs to
                        var conversation = database.getConversationById(messageRequest.getConversationId());

                        if (conversation != null) {
                            Message message = new Message();
                            message.setId(database.nextMessageId(conversation.getId()));
                            message.setSender(database.getUserById(messageRequest.getSenderId()));
                            message.setContent(messageRequest.getMessageContent());

                            // Add message to database
                            conversation.getMessages().add(message);
                            writeDatabase();

                            // Broadcast message to currently actve users
                            for (User user : conversation.getParticipants()) {
                                int userIndex = users.indexOf(user);
                                if (userIndex != -1) {
                                    DataOutputStream clientOut = new DataOutputStream(clients.get(userIndex).getOutputStream());
                                    clientOut.writeUTF(
                                            mapper.writeValueAsString(
                                                    new Response(ResponseType.NEW_MESSAGE, "Message incoming")
                                            )
                                    );
                                    System.out.println("Convo id: " + conversation.getId());
                                    System.out.println("User id: " + user.getId());
                                    clientOut.writeUTF(mapper.writeValueAsString(database.getConversationData(conversation)));
                                }
                            }
                        }
                        break;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
