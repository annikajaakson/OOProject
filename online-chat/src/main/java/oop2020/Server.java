package oop2020;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

class socketThread implements Runnable{

    private final Socket socket;
    private DataInputStream in;
    private DataOutputStream out;

    public socketThread(Socket socket, DataInputStream in, DataOutputStream out){
        this.socket = socket;
        this.in = in;
        this.out = out;
    }

    public List<String> readData(String user) throws IOException {
        //Array of data from the specific user data file
        List<String> data = new ArrayList<>();

        String userFormat = user.replace(" ", "").toLowerCase();

        String workingDir = System.getProperty("user.dir");
        String dataDir = workingDir + "/data/";
        File dir = new File(dataDir);

        //Makes directory if absent
        dir.mkdir();

        String fileName = userFormat + ".txt";
        File txt = new File(dataDir, fileName);
        boolean fileReturn = txt.createNewFile();

        //If file exists
        if(!fileReturn){
            try (Scanner s = new Scanner(new FileReader(dataDir +fileName))) {
                while (s.hasNext()) {
                    data.add(s.nextLine());
                }
            }
        }

        return data;
    }

    public void run() {
        try{
            switch (in.readInt()){
                case 0:
                    try {
                        //Read who connected
                        String user = in.readUTF();
                        System.out.println("user " + user + " connected");

                        //Read data from database
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
                    }break;
                default:
                    try {
                        throw new IllegalAccessException("Illegal prefix");
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}

public class Server {

    public static void main(String[] args) throws IOException {

        int port = 1337;

        try (ServerSocket ss = new ServerSocket(port)) {
            while(true){
                try{
                    Socket socket = ss.accept();
                    DataInputStream in = new DataInputStream(socket.getInputStream());
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream());

                    Thread thread = new Thread(new socketThread(socket, in, out));
                    thread.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}