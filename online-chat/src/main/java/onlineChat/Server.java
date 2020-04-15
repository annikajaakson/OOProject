package onlineChat;

import com.fasterxml.jackson.databind.ObjectMapper;
import components.chat.User;
import components.database.Database;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    private static String databasePath = "db-files/database.json";

    public static void main(String[] args) throws IOException {
        Path DBPath = Paths.get(databasePath);

        int port = 1337;

        // Threadsafe list implementation
        // TODO: Remove client from list on logout
        List<Socket> clients = new CopyOnWriteArrayList<>();
        List<User> users = new CopyOnWriteArrayList<>();

        try (ServerSocket ss = new ServerSocket(port)) {
            Database database = new ObjectMapper().readValue(new File(DBPath.toString()), Database.class);

            while (true) {
                try {
                    Socket socket = ss.accept();
                    DataInputStream in = new DataInputStream(socket.getInputStream());
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());

                    Thread thread = new Thread(new ServerThread(socket, in, out, users, clients, database, DBPath));
                    thread.start();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}