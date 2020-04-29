package onlineChat;

import com.fasterxml.jackson.databind.ObjectMapper;
import components.chat.Conversation;
import components.chat.Message;
import components.chat.User;
import components.database.Database;
import components.request.*;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import javax.mail.*;
import javax.mail.internet.*;

public class UserThread implements Runnable {

    // Communication with sockets
    private final Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    // List of clients as user objects and their corresponding sockets
    private List<User> users;
    private List<Socket> clients;

    // Database variables
    private Path DBPath;
    private Database database;
    private ObjectMapper mapper = new ObjectMapper();

    // Email sending
    private String username = "fakemessengeroop@gmail.com";
    private String password = "Fak3M3ss3ng3r";

    public UserThread(Socket socket, DataInputStream in, DataOutputStream out, List<User> users, List<Socket> clients, Database database, Path DBPath) {
        this.socket = socket;
        this.in = in;
        this.out = out;
        this.users = users;
        this.clients = clients;
        this.database = database;
        this.DBPath = DBPath;
    }

    public synchronized Database fetchDatabase() throws IOException {
        return mapper.readValue(new File(DBPath.toString()), Database.class);
    }

    public synchronized void writeDatabase() throws IOException {
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(DBPath.toString()), database);
    }

    public void sendEmail(String sender, String senderPwd, String[] recipients, String subject, String body) {
        // If there are no people to send the email to
        if (recipients.length == 0) return;

        Properties props = System.getProperties();
        String host = "smtp.gmail.com";
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.user", sender);
        props.put("mail.smtp.password", senderPwd);
        props.put("mail.smtp.auth", "plain");

        Session session = Session.getDefaultInstance(props);
        MimeMessage message = new MimeMessage(session);

        try {
            // Set message origin (sender)
            message.setFrom(new InternetAddress(sender));

            // Add all recipients to the message
            for (String recipient : recipients) {
                message.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(recipient));
            }

            // Set message subject and body
            message.setSubject(subject);
            message.setText(body);

            // Send the message
            Transport transport = session.getTransport("smtp");
            transport.connect(host, sender, senderPwd);
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();

            System.out.println("Email sent successfully!");
        } catch (MessagingException e) {
            System.out.println("ERROR: " + e.getMessage());
        }
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

                        System.out.println("Message request from user " + messageRequest.getSenderId());

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

                            // List of emails to send notification of new message to
                            List<String> inactiveUserEmails = new ArrayList<>();
                            // Broadcast message to currently actve users
                            for (User user : conversation.getParticipants()) {
                                int userIndex = users.indexOf(user);

                                // If user is not currently connected
                                if (userIndex == -1) {
                                    inactiveUserEmails.add(user.getEmail());
                                    System.out.println("Offline: " + user.getEmail());
                                    continue;
                                }

                                // If user is currently connected
                                DataOutputStream clientOut = new DataOutputStream(clients.get(userIndex).getOutputStream());
                                clientOut.writeUTF(
                                        mapper.writeValueAsString(
                                                new Response(ResponseType.NEW_MESSAGE, "Message incoming")
                                        )
                                );
                                clientOut.writeUTF(mapper.writeValueAsString(database.getConversationData(conversation)));
                            }

                            String emailContent = "You have an unread message from "
                                    + message.getSender().getUsername()
                                    + ". Log in to Fake Messenger to read it.\n\n"
                                    + "Best regards\nFake Messenger team\n";
                            // Send emails to currently inactive users
                            sendEmail(
                                    this.username,
                                    this.password,
                                    inactiveUserEmails.toArray(new String[0]),
                                    "New Message from " + message.getSender().getUsername(),
                                    emailContent
                            );
                        }
                        break;
                    case CONVERSATION:
                        var convoRequest = mapper.readValue(objectAsString, ConversationRequest.class);

                        System.out.println("Conversation request received");

                        // Create new conversation
                        Conversation newConvo = new Conversation();
                        newConvo.setId(database.nextConversationId());
                        newConvo.setMessages(new ArrayList<>());
                        // Add participants to conversation
                        newConvo.setParticipants(convoRequest.getParticipantNames().stream()
                                .map(name -> database.getUserByName(name))
                                .collect(Collectors.toList()));

                        // Add new conversation to database
                        database.getConversations().add(newConvo);

                        // Get subdatabase of all the necessary info for the conversation
                        Database convoData = database.getConversationData(newConvo);

                        // List of emails to send notification of new conversation to
                        List<String> inactiveUserEmails = new ArrayList<>();
                        // Broadcast new conversation to all participants
                        for (User participant : newConvo.getParticipants()) {
                            int userIndex = users.indexOf(participant);

                            // If user is not connected
                            if (userIndex == -1) {
                                inactiveUserEmails.add(participant.getEmail());
                                System.out.println("Offline: " + participant.getEmail());
                                continue;
                            }

                            // If user is online
                            DataOutputStream clientOut = new DataOutputStream(clients.get(userIndex).getOutputStream());
                            // Send conversation data to socket
                            clientOut.writeUTF(mapper.writeValueAsString(
                                    new Response(ResponseType.NEW_CONVERSATION, "New conversation data on the way"))
                            );
                            clientOut.writeUTF(mapper.writeValueAsString(convoData));
                        }

                        String emailContent = "You have been added to a conversation with "
                                + newConvo.getParticipants().stream().map(User::getUsername).collect(Collectors.joining(", "))
                                + ". Log in to Fake Messenger to see it.\n\n"
                                + "Best regards\nFake Messenger team\n";
                        // Send emails to currently inactive users
                        sendEmail(
                                this.username,
                                this.password,
                                inactiveUserEmails.toArray(new String[0]),
                                "You have been added to a conversation",
                                emailContent
                        );
                        break;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
