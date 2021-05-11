/**
 * The JoinWhiteBoard programs extends with the CreateWhiteBoard to
 * empower the ability for new users to join the shared whiteboard
 * and be able to draw simultaneously with a reasonable delay.
 *
 * @author  Zhonglin Shi
 * @studentNumber 774355
 * @version 0.1
 * @since   2020-05-30
 */

import javax.swing.*;
import java.io.*;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;

public class JoinWhiteBoard {

    // IP and port
    private static String IPAddress = "";
    private static int port = 0;
    private static String username = null;

    private static String response = null;

    private static final Integer MAX_ARGS = 3;

    private static final Integer[] PORT_LIMIT = {1024, 65535};

    private static WhiteBoardGUI board;

    private static Socket clientSocket = null;
    private static DataInputStream is;
    private static DataOutputStream os;
    private static ObjectInputStream objIn;
    private static ObjectOutputStream objOut;

    public static void main(String[] args)
    {
        // Test if the command line parameters are entered in a correct format and order
        try {
            IPAddress = args[0];
            port = Integer.parseInt(args[1]);
            username = args[2];

            // check args
            if (args.length > MAX_ARGS) {
                throw new NumberFormatException("Too many args. Expected: <IP Address> <port> username");
            } else if (args.length < MAX_ARGS) {
                throw new NumberFormatException("Not enough args. Expected: <IP Address> <port> username");
            }

            // check port
            if (port < PORT_LIMIT[0] && port > PORT_LIMIT[1] ) {
                throw new ArrayIndexOutOfBoundsException();
            }

            // check if server address is reachable
            if (!InetAddress.getByName(IPAddress).isReachable(5000) ) {
                throw new UnknownHostException("Server address not reachable");
            }

            try {
                clientSocket = new Socket(IPAddress, port);
                is = new DataInputStream(clientSocket.getInputStream());;
                os = new DataOutputStream(new BufferedOutputStream(clientSocket.getOutputStream()));

            } catch (UnknownHostException | ConnectException e) {
                JOptionPane.showMessageDialog(null, "Server is not started yet. Please wait until the server is started!");
                System.exit(0);
            } catch (IOException e) {
                ExceptionHandler.main(e);
            }

            String[] requestDetail= {"join", username};
            response = request(requestDetail, is, os);

            String[] responseList = response.split("(\\s)*::(\\s)*");
            String responseType = responseList[0];

            ArrayList<String> userList = new ArrayList<>();

            for(int i = 1; i < responseList.length; i++) {
                userList.add(responseList[i]);
            }

            switch (responseType) {
                case "success":
                    // Initiate GUI
                    System.out.println("success connected to board");
                    try {
                        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    board = new WhiteBoardGUI(username, "client", clientSocket, userList);

                    board.requestForGraphics();
                    board.receiveData();
                    break;
                case "rejected":
                    JOptionPane.showMessageDialog(null, "The manager has rejected your request to join, please try again later");
                    System.exit(0);
                    break;
                case "nameRejected":
                    JOptionPane.showMessageDialog(null, "Your username is already existed in the whiteboard. Please try to rejoin with a different username. E.g. " + username + "2");
                    System.exit(0);
                    break;
            }


        } catch (NumberFormatException | ArrayIndexOutOfBoundsException | IOException | ClassNotFoundException e) {
            ExceptionHandler.main(e);

        }


    }

    public static String request(String[] data, DataInputStream input, DataOutputStream output) {
        String response = "";

        try {
            String sendData = String.join("::", data);

            output.writeUTF(sendData);
            System.out.println("Data sent to Server --> " + sendData);
            output.flush();


            // obtain all data from the stream
            while (true){
                if (input.available() > 1) {
                    response = input.readUTF();
                    break;
                }
            }


        } catch (IOException e) {
            ExceptionHandler.main(e);
        }
        return response;
    }

}
