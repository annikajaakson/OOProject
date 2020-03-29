package onlineChat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Client {
    public static void main(String[] args) throws Exception{

        List<String> userData = new ArrayList<>();

        String user = "Peeter Peet";

        int port = 1337;

        System.out.println("connecting to server");
        System.out.println("...");

        try (Socket socket = new Socket("localhost", port); DataInputStream in = new DataInputStream(socket.getInputStream()); DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

            System.out.println("connected");

            //Just read the data
            int action = 0;

            try{
                switch (action){
                    case 0:
                        out.writeInt(action);

                        out.writeUTF(user);

                        int dataSize = in.readInt();

                        for (int j = 0; j < dataSize; j++) {
                            userData.add(in.readUTF());
                        }

                        System.out.println("user data present:");
                        System.out.println(userData);
                        break;
                    default:
                        try {
                            throw new IllegalAccessException("Illegal prefix");
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }break;
                }
            }catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}