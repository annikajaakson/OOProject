package onlineChat;

import com.fasterxml.jackson.databind.ObjectMapper;
import components.chat.Conversation;
import components.chat.User;
import components.database.Database;
import components.request.Response;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;

public class ClientThread implements Runnable {

    private User currentUser;
    private final Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private ObjectMapper mapper = new ObjectMapper();

    public ClientThread(User currentUser, Socket socket, DataInputStream in, DataOutputStream out) {
        this.currentUser = currentUser;
        this.socket = socket;
        this.in = in;
        this.out = out;
    }

    public void run() {
        try {
            while (true) {
                var response = mapper.readValue(in.readUTF(), Response.class);
                System.out.println(response.getErrorMsg());

                Database conversationData;
                switch (response.getResponseType()) {
                    case NEW_MESSAGE:
                        conversationData =  mapper.readValue(in.readUTF(), Database.class);
                        Conversation updatedConversation = conversationData.getConversations().get(0);
                        int replacedConvoIndex = currentUser.getConversations().stream()
                                .filter(convo -> convo.getId() == updatedConversation.getId())
                                .map(convo -> currentUser.getConversations().indexOf(convo))
                                .findFirst()
                                .orElse(-1);

                        currentUser.getConversations().set(replacedConvoIndex, updatedConversation);
                        break;
                    case NEW_CONVERSATION:
                        conversationData =  mapper.readValue(in.readUTF(), Database.class);
                        Conversation newConversation = conversationData.getConversations().get(0);
                        currentUser.getConversations().add(newConversation);
                        break;
                }
            }
        } catch (EOFException e) {
            System.out.println("Closing client connection");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
