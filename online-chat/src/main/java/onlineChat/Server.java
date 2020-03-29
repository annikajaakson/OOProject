package onlineChat;

import components.database.Database;
import components.chat.User;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

class socketThread implements Runnable {

    private final Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private Database database;
    private ObjectMapper mapper;

    public socketThread(Socket socket, DataInputStream in, DataOutputStream out, Database database, ObjectMapper mapper) {
        this.socket = socket;
        this.in = in;
        this.out = out;
        this.database = database;
        this.mapper = mapper;
    }

    public List<String> readData(String user) throws IOException {
        // Array of data from the specific user data file
        List<String> data = new ArrayList<>();

        String userFormat = user.replace(" ", "").toLowerCase();

        String workingDir = System.getProperty("user.dir");
        String dataDir = workingDir + "/data/";
        File dir = new File(dataDir);

        // Makes directory if absent
        dir.mkdir();

        String fileName = userFormat + ".txt";
        File txt = new File(dataDir, fileName);
        boolean fileReturn = txt.createNewFile();

        // If file exists
        if (!fileReturn) {
            try (Scanner s = new Scanner(new FileReader(dataDir +fileName))) {
                while (s.hasNext()) {
                    data.add(s.nextLine());
                }
            }
        }

        return data;
    }

    public void run() {
        try {
            switch (in.readInt()) {
                case 0:
                    try {
                        // Read who connected
                        String user = in.readUTF();
                        System.out.println("user " + user + " connected");

                        // Read data from database
                        List<String> userData = readData(user);

                        int preFix = userData.size();
                        out.writeInt(preFix);

                        for (String userDatum : userData) {
                            out.writeUTF(userDatum);
                        }

                        System.out.println("user information presented for " + user);
                        System.out.println();

                        this.socket.close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                // Login event occured
                case 1:
                    try {
                        // Read who connected
                        String username = in.readUTF();
                        System.out.println("Login attempt with username " + username);

                        String password = in.readUTF();

                        User connectedUser = null;

                        for (User user : database.getUsers()) {
                            // If user exists, check password
                            if (user.getUsername().equals(username)) {
                                connectedUser = user;

                                // If password is correct
                                if (password.equals(user.getPassword())) {
                                    out.writeInt(0);
                                    out.writeUTF("Login successful");
                                } else {
                                    out.writeInt(1);
                                    out.writeUTF("Wrong password");
                                }
                                break;
                            }
                        }

                        if (connectedUser == null) {
                            out.writeInt(1);
                            out.writeUTF("Unknown user");
                        }

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                // Register event occured
                case 2:
                    try {
                        // Read who is trying to register
                        String username = in.readUTF();
                        System.out.println("Register request with username " + username);

                        // Check if username is taken
                        boolean userNameTaken = false;
                        for (User user : database.getUsers()) {
                            if (user.getUsername().equals(username)) {
                                out.writeInt(1);
                                out.writeUTF("Username is taken");
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
                            newUser.setPassword(in.readUTF());
                            newUser.setEmail(in.readUTF());

                            // Add user to database
                            database.getUsers().add(newUser);
                            mapper.writerWithDefaultPrettyPrinter().writeValueAsString(database);

                            out.writeInt(0);
                            out.writeUTF("New user registered");
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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

public class Server {
    private static String databasePath = "/database.json";

    public static void main(String[] args) throws IOException, URISyntaxException {
        Path DBPath = Paths.get(Server.class.getResource(databasePath).toURI());

        ObjectMapper mapper = new ObjectMapper();
        Database database = mapper.readValue(new File(DBPath.toString()), Database.class);

        int port = 1337;

        try (ServerSocket ss = new ServerSocket(port)) {
            while (true) {
                try {
                    Socket socket = ss.accept();
                    DataInputStream in = new DataInputStream(socket.getInputStream());
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());

                    Thread thread = new Thread(new socketThread(socket, in, out, database, mapper));
                    thread.start();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}