/**
 * The CreateWhiteBoard uses TCP socket connection to create a distributed shared
 * whiteboard which allows multiple users to join.
 * As the first user in whiteboard, the user will be the manager and entitled with the
 * ability to save, load, saves, and exit the program. Also, the manager is entitled with
 * the ability to remove specific users from the whiteboard apart from himself.
 *
 * @author  Zhonglin Shi
 * @studentNumber 774355
 * @version 0.1
 * @since   2020-05-30
 */


import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.util.ArrayList;
import javax.net.ServerSocketFactory;
import javax.swing.*;

public class CreateWhiteBoard {

    // Declare the port number
    private static int port = 0;
    private static String username = null;
    private static String IPAddress = null;
    private static ArrayList<User> users = new ArrayList<>();
    private static WhiteBoardGUI board;

    // Identifies dictionary path
    private static String dictPath = "";

    private static final Integer MAX_ARGS = 3;

    private static final Integer[] PORT_LIMIT = {1024, 65535};

    public static void main(String[] args)
    {
        // Test if the command line parameters are entered in a correct format and order
        try {
            IPAddress = args[0];
            port = Integer.parseInt(args[1]);
            username = args[2];


            if (args.length > MAX_ARGS) {
                throw new NumberFormatException("Too many args. Expected: <IP Address> <port> username");
            } else if (args.length < MAX_ARGS) {
                throw new NumberFormatException("Not enough args. Expected: <IP Address> <port> username");
            }

            if (port < PORT_LIMIT[0] && port > PORT_LIMIT[1] ) {
                throw new ArrayIndexOutOfBoundsException();
            }



        }
        catch (ArrayIndexOutOfBoundsException ae) {
           ExceptionHandler.main(ae);
        }
        catch (NumberFormatException ne) {
            ExceptionHandler.main(ne);
        }

        Thread wb = new Thread(() -> {
            try {
                WhiteBoardService();
            } catch (IOException e) {
                ExceptionHandler.main(e);
            } catch (ClassNotFoundException e) {
                ExceptionHandler.main(e);
            }
        });
        wb.start();

    }

    public static void WhiteBoardService() throws IOException, ClassNotFoundException {
        ServerSocketFactory factory = ServerSocketFactory.getDefault();
        User.addInUserList(new User(username, null, "manager"));
        board = new WhiteBoardGUI(username, "manager", User.UsernameList());
        ServerSocket server = factory.createServerSocket(port);
        try
        {

            System.out.println("Waiting for client connection ...");

            // Wait for connections.
            while(true)
            {
                Socket client = server.accept();

                // Start a new thread for a connection
                Thread t = new DrawThread(client);
                t.start();
            }

        }
        catch (IOException e)
        {
            ExceptionHandler.main(e);
        }
    }

    public static void Broadcast(DrawService ds, Socket clientSocket, String type, String thatUser) throws IOException {

        DataOutputStream os;
        for(User eachUser:User.curUserList) {
            if (!eachUser.role.equals("manager")) {
                if (clientSocket == eachUser.userSocket) {
                    continue;
                }
                os = new DataOutputStream(new BufferedOutputStream(eachUser.userSocket.getOutputStream()));
                String data = "";
                switch (type) {
                    case "draw":
                        data = ds.x1 + "," + ds.y1 + "," + ds.x2 + "," + ds.y2 + "," + ds.type + "," + ds.s1;
                        break;
                    case "join":
                        data = "newuser," + thatUser;
                        break;
                    case "leave":
                        data = "userleave," + thatUser;
                        break;
                    case "cleanUp":
                        data = "cleanup,";
                        break;
                    case "load":
                        data = "load,";
                        break;
                }

                os.writeUTF(data);
                os.flush();
            }

        }

    }

    public static void BroadcastGraphic(DataOutputStream os) throws IOException {
        StringBuilder data = new StringBuilder("graphics");
        for (DrawService draw:board.draws) {
            if (draw != null) {
                String oneDraw = draw.x1 + "|" + draw.y1 + "|" + draw.x2 + "|" + draw.y2 + "|" + draw.type + "|" + draw.s1;
                data.append(",").append(oneDraw);
            }

        }
        os.writeUTF(String.valueOf(data));
        os.flush();
    }

    public static void kick(Object user) throws IOException {
        boolean exist = false;
        System.out.println("start kicking...");
        String kickedUser = user.toString();
        int quitIndex = 0;
        for(User eachUser:User.curUserList) {
            if (!eachUser.role.equals("manager") && eachUser.username.equals(kickedUser)) {
                exist = true;
                quitIndex = User.curUserList.indexOf(eachUser);
            }
        }

        if(exist) {
            System.out.println("connection closed for: " + kickedUser);
            board.removeUser(kickedUser);
            Broadcast(null, null, "leave", kickedUser);
            if (quitIndex > 0) {
                User.removeFromUserList(quitIndex);
            }
        }
    }



    static class DrawThread extends Thread {
        Socket client;
        DataInputStream is;
        DataOutputStream os;
        Boolean flag = false;

        DrawThread(Socket client) {
            this.client = client;

        }

        public void run() {
            try {
                os = new DataOutputStream(new BufferedOutputStream(client.getOutputStream()));
                is = new DataInputStream(new BufferedInputStream(client.getInputStream()));

                if(!flag){
                    serveClient(this.client, is, os);
                    flag = true;
                }


                while (true) {

                    String test = is.readUTF();
                    String[] data = test.split(",");

                    if (data[0].equals("graphics")) {
                        BroadcastGraphic(os);

                    } else if(data.length == 6) {
                        DrawService newDraw = new DrawService();
                        newDraw.x1 = Integer.parseInt(data[0]);
                        newDraw.y1 = Integer.parseInt(data[1]);
                        newDraw.x2 = Integer.parseInt(data[2]);
                        newDraw.y2 = Integer.parseInt(data[3]);
                        newDraw.type = data[4];
                        newDraw.s1 = data[5];
                        board.createDrawForClient(newDraw);

                        Broadcast(newDraw, this.client, "draw", null);

                    }
                }

            } catch (EOFException e) {
                Object quitName = null;
                for(User eachUser:User.curUserList) {
                    if (eachUser.userSocket == client) {
                        quitName = eachUser.username;
                        break;
                    }
                }

                try {
                    kick(quitName);
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }

            } catch (IOException e) {
                // User quit will cause exception
//                ExceptionHandler.main(e);
                System.out.println("connection reset");
            }
        }


    }


    private static void serveClient(Socket clientSocket, DataInputStream input, DataOutputStream output)
    {
        try
        {

            String request = input.readUTF();
            System.out.println("CLIENT: " + clientSocket.getInetAddress().getHostName() + " " + clientSocket.getLocalPort() + " " + request);
            String[] requestType = request.split("(\\s)*::(\\s)*");
            String clientUsername = requestType[1];

            // Handle different types of request from clients
            switch(requestType[0]) {
                case "join":
                    int response = JOptionPane.showConfirmDialog(null, clientUsername + " want to join in?", "Authentication",
                            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (response == JOptionPane.YES_OPTION) {
                        boolean canJoin = false;
                        for(User eachUser:User.curUserList) {
                            if (eachUser.username.equals(clientUsername)) {
                                canJoin = false;
                                break;
                            } else {
                                canJoin = true;
                            }
                        }

                        if(canJoin) {
                            User.addInUserList(new User(clientUsername, clientSocket, "client"));
                            ArrayList<String> curUsers = User.UsernameList();
                            curUsers.add(0, "success");
                            String responseData = String.join("::", curUsers);
                            board.addNewUser(clientUsername);
                            output.writeUTF(responseData);
                            Broadcast(null, clientSocket, "join", clientUsername);
                        } else {
                            output.writeUTF("nameRejected::");
                        }

                    } else if (response == JOptionPane.NO_OPTION) {
                        JOptionPane.showMessageDialog(null, clientUsername + "has been rejected to join in.");
                        output.writeUTF("rejected::");
                    }
                    output.flush();
                    break;
            }


        }
        catch (IOException e)
        {
            ExceptionHandler.main(e);

        } catch (Exception e) {
            ExceptionHandler.main(e);
        }
    }

}
