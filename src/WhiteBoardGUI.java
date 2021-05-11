/**
 * The WhiteBoardGUI program creates an interface for the manager and the users
 * of the shared whiteboard to be able to draw on the board with several default
 * tools: free-hand pencil, rectangle, line, circle and text
 * All users will be able to see a user list which contains all the users in the
 * whiteboard as well.
 *
 * @author  Zhonglin Shi
 * @studentNumber 774355
 * @version 0.1
 * @since   2020-05-30
 */

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;

public class WhiteBoardGUI extends JFrame {

    private ObjectInputStream input;
    private ObjectOutputStream output;
    private static String username;
    private static String role;
    private Socket client;
    private JButton tools[];
    private String toolNames[] = {
            "pencil",
            "line",
            "rectangle",
            "circle",
            "text"
    };
    private JLabel statusPanel;
    private Icon icons[];
    private String tooltips[] = {
            "Free hand drawing",
            "Draw a line",
            "Draw a rectangle",
            "Draw a circle",
            "Enter a text"
    };
    private String SavedFileName;
    private File filePath;
    private Boolean saveAs = false;
    private Boolean saved = false;

    private int width = 800, height = 700;
    private DrawPanel drawPanel;
    DataStream drawRecord = new DataStream();
    DrawService[] draws = new DrawService[drawRecord.maxStorage];
    private String drawingTool = "pencil";
    DrawService newDraw = null;
    ObjectOutputStream ObjOut;
    ObjectInputStream ObjIn;
    DataOutputStream os;
    DataInputStream is;

    DefaultListModel model = new DefaultListModel();
    private JPanel userListGUI = new JPanel();
    private JList userListOnBoard = new JList();
    private JButton kickOut = new JButton("kick");

    private ArrayList<String> curUserList = new ArrayList<>();

    public WhiteBoardGUI(String username, String role, ArrayList<String> userList) throws IOException, ClassNotFoundException {
        super("Shared Whiteboard for " + username);
        this.username = username;
        this.role = role;
        curUserList = userList;
        initialise();

    }

    public WhiteBoardGUI(String username, String role, Socket client, ArrayList<String> userList) throws IOException, ClassNotFoundException {
        super("Shared Whiteboard for " + username);
        os = new DataOutputStream(new BufferedOutputStream(client.getOutputStream()));
        is = new DataInputStream(new BufferedInputStream(client.getInputStream()));
        this.username = username;
        this.client = client;
        this.role = role;
        curUserList = userList;

        initialise();

    }

    public void initialise() {
        JMenuBar bar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('F');
        JMenuItem newItem = new JMenuItem("New");
        newItem.setMnemonic('N');
        newItem.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        try {
                            newFile();
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                        }
                    }
                });
        fileMenu.add(newItem);
        JMenuItem saveItem = new JMenuItem("Save");
        saveItem.setMnemonic('S');
        saveItem.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        saveAs = false;
                        saveFile();
                        saved = true;
                    }
                });
        fileMenu.add(saveItem);
        JMenuItem saveItemAs = new JMenuItem("SaveAs");
        saveItemAs.setMnemonic('A');
        saveItemAs.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        saveAs = true;
                        saved = false;
                        saveFile();
                        saved = true;

                    }
                });
        fileMenu.add(saveItemAs);
        JMenuItem loadItem = new JMenuItem("Load");
        loadItem.setMnemonic('L');
        loadItem.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        loadFile();
                        saveAs = false;
                        saved = true;
                    }
                });
        fileMenu.add(loadItem);
        fileMenu.addSeparator();
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.setMnemonic('X');
        exitItem.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        exit();
                    }
                });
        fileMenu.add(exitItem);
        bar.add(fileMenu);

        JToolBar buttonPanel = new JToolBar(JToolBar.HORIZONTAL);
        tools = new JButton[toolNames.length];

        for (int i = 0; i < toolNames.length; i++) {
            ImageIcon icon = new ImageIcon(WhiteBoardGUI.class.getResource("img/" + toolNames[i] + ".gif"));
            tools[i] = new JButton("", icon);
            tools[i].setToolTipText(tooltips[i]);
            buttonPanel.add(tools[i]);
        }

        ButtonEventHandler eventHandler = new ButtonEventHandler();

        for (int i = 0; i < tools.length; i++) {
            tools[i].addActionListener(eventHandler);
        }

        Container c = getContentPane();
        if (role.equals("manager")) {
            super.setJMenuBar(bar);
        }

        statusPanel = new JLabel();
        drawPanel = new DrawPanel();


        // create user list GUI
        String[] userArr = new String[curUserList.size()];
        userArr = curUserList.toArray(userArr);

        for(int i = 0; i < userArr.length; i++) {
            model.addElement(userArr[i]);
        }
        userListOnBoard = new JList(model);

        String title = "Participants";
        Border border = BorderFactory.createTitledBorder(title);
        userListGUI.setLayout(new BorderLayout());
        userListGUI.setBorder(border);
        userListGUI.add(userListOnBoard, BorderLayout.CENTER);
        if(role.equals("manager")) {
            userListGUI.add(kickOut, BorderLayout.SOUTH);
            kickOut.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if(userListOnBoard.getSelectedValue() != null && !userListOnBoard.getSelectedValue().equals(username)) {
                        try {
                            CreateWhiteBoard.kick(userListOnBoard.getSelectedValue());
                        } catch (IOException ioException) {
                            ioException.printStackTrace();
                        }

                    }
                }
            });
        }
        userListGUI.setPreferredSize(new Dimension(100, 700));

        c.add(buttonPanel, BorderLayout.NORTH);
        c.add(drawPanel, BorderLayout.CENTER);
        c.add(statusPanel, BorderLayout.SOUTH);
        c.add(userListGUI, BorderLayout.EAST);

        createDraw();
        setSize(width, height);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setVisible(true);


    }

    class DrawPanel extends JPanel {
        public DrawPanel() {
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
            setBackground(Color.white);
            addMouseListener(new MouseMoveOrClick());
            addMouseMotionListener(new MouseDragOrMove());
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            int j = 0;
            while (j <= drawRecord.curIndex) {
                draw(g2d, draws[j]);
                j++;
            }
        }

        void draw(Graphics2D g2d, DrawService ds) {
            if (ds != null) {
                ds.draw(g2d);
            }
        }
    }

    class MouseMoveOrClick extends MouseAdapter {

        public void mousePressed(MouseEvent e) {
            statusPanel.setText("     Mouse Pressed :(" + e.getX() +
                    ", " + e.getY() + ")");
            draws[drawRecord.curIndex].x1 = draws[drawRecord.curIndex].x2 = e.getX();
            draws[drawRecord.curIndex].y1 = draws[drawRecord.curIndex].y2 = e.getY();

            if(drawingTool.equals("pencil")) {
                draws[drawRecord.curIndex].x1 = draws[drawRecord.curIndex].x2 = e.getX();
                draws[drawRecord.curIndex].y1 = draws[drawRecord.curIndex].y2 = e.getY();
                drawRecord.nextIndex();
                createDraw();
            }
            if (drawingTool.equals("text")) {
                draws[drawRecord.curIndex].x1 = e.getX();
                draws[drawRecord.curIndex].y1 = e.getY();
                String input;
                input = JOptionPane.showInputDialog(
                        "Please input the text you want!");
                draws[drawRecord.curIndex].s1 = input;
                newDraw = draws[drawRecord.curIndex];
                drawRecord.nextIndex();
                createDraw();
                repaint();
                sendToServer();
            }
        }

        public void mouseReleased(MouseEvent e) {
            statusPanel.setText("     Mouse Released :(" + e.getX() +
                    ", " + e.getY() + ")");
            if (drawingTool.equals("pencil")) {
                draws[drawRecord.curIndex].x1 = e.getX();
                draws[drawRecord.curIndex].y1 = e.getY();
            }
            draws[drawRecord.curIndex].x2 = e.getX();
            draws[drawRecord.curIndex].y2 = e.getY();
            newDraw = draws[drawRecord.curIndex];
            repaint();
            drawRecord.nextIndex();
            createDraw();
            sendToServer();
        }

        public void mouseEntered(MouseEvent e) {
            statusPanel.setText("     Mouse Entered :(" + e.getX() +
                    ", " + e.getY() + ")");
        }

        public void mouseExited(MouseEvent e) {
            statusPanel.setText("     Mouse Exited :(" + e.getX() +
                    ", " + e.getY() + ")");
        }


    }

    class MouseDragOrMove implements MouseMotionListener {
        public void mouseDragged(MouseEvent e) {
            statusPanel.setText("     Mouse Dragged :(" + e.getX() +
                    ", " + e.getY() + ")");
            if (drawingTool.equals("pencil")) {
                draws[drawRecord.curIndex - 1].x1 = draws[drawRecord.curIndex].x2 = draws[drawRecord.curIndex].x1 = e.getX();
                draws[drawRecord.curIndex - 1].y1 = draws[drawRecord.curIndex].y2 = draws[drawRecord.curIndex].y1 = e.getY();
                newDraw = draws[drawRecord.curIndex];
                drawRecord.nextIndex();
                createDraw();
                sendToServer();
            } else {
                draws[drawRecord.curIndex].x2 = e.getX();
                draws[drawRecord.curIndex].y2 = e.getY();
            }

            repaint();
        }
        public void mouseMoved(MouseEvent e) {
            statusPanel.setText("     Mouse Moved :(" + e.getX() +
                    ", " + e.getY() + ")");
        }

    }

    void sendToServer() {
        try {
            if (!role.equals("manager")){
                sendData(newDraw);
            }
            else {
                CreateWhiteBoard.Broadcast(newDraw, this.client, "draw", null);
            }
        } catch (IOException e) {
            ExceptionHandler.main(e);
        }
    }

    public void sendData(DrawService newDraw) throws IOException {
        if(newDraw != null && newDraw.x1 != 0 && newDraw.y1 != 0) {
            String data = newDraw.x1 + "," + newDraw.y1 + "," + newDraw.x2 + "," + newDraw.y2 + "," + newDraw.type + "," + newDraw.s1;
            os.writeUTF(data);
            os.flush();
        }
    }

    public void requestForGraphics() throws IOException {
        os.writeUTF("graphics,");
        os.flush();
    }

    public void receiveData() throws IOException, ClassNotFoundException {
        while (true) {
            try {

                String received = is.readUTF();
                System.out.println("!!"+received);
                String[] data = received.split(",");
                if (data[0].equals("newuser")) {
                    addNewUser(data[1]);
                } else if (data[0].equals("userleave")) {
                    if (data[1].equals(username)) {
                        JOptionPane.showMessageDialog(null, "Oops! You have been kicked out by the white board owner. Please contact the owner or restart the white board.");
                        System.exit(0);
                    } else {
                        removeUser(data[1]);
                    }
                } else if (data[0].equals("graphics")) {
                    // redraw
                    for (int i = 1; i < data.length - 1; i++) {
                        String[] drawData = data[i].split("\\|");
                        DrawService newDraw = new DrawService();
                        newDraw.x1 = Integer.parseInt(drawData[0]);
                        newDraw.y1 = Integer.parseInt(drawData[1]);
                        newDraw.x2 = Integer.parseInt(drawData[2]);
                        newDraw.y2 = Integer.parseInt(drawData[3]);
                        newDraw.type = drawData[4];
                        newDraw.s1 = drawData[5];
                        createDrawForClient(newDraw);
                    }

                } else if (data[0].equals("cleanup")) {
                    cleanBoard();
                } else if (data[0].equals("load")) {
                    cleanBoard();
                    requestForGraphics();
                } else {
                    if(data.length == 6) {
                        DrawService newDraw = new DrawService();
                        newDraw.x1 = Integer.parseInt(data[0]);
                        newDraw.y1 = Integer.parseInt(data[1]);
                        newDraw.x2 = Integer.parseInt(data[2]);
                        newDraw.y2 = Integer.parseInt(data[3]);
                        newDraw.type = data[4];
                        newDraw.s1 = data[5];
                        createDrawForClient(newDraw);
                    }
                }

            } catch (EOFException | SocketException e) {
                JOptionPane.showMessageDialog(null, "Server is closed by the manager. You have been removed from this whiteboard");
                System.exit(0);
            }

        }
    }

    public class ButtonEventHandler implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            for (int i = 0; i < tools.length; i++) {
                if (e.getSource() == tools[i]) {
                    drawingTool = toolNames[i];
                    createDraw();
                    repaint();
                }
            }
        }
    }



    void createDraw() {
        if (drawingTool.equals("text")) {
            drawPanel.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));
        } else {
            drawPanel.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        }

        ModeSwitcher();
        draws[drawRecord.curIndex].type = drawingTool;
        System.out.println(drawRecord.curIndex + "=" + drawingTool);

    }

    void ModeSwitcher() {
        switch (drawingTool) {
            case "pencil":
                draws[drawRecord.curIndex] = new Pencil();
                break;
            case "line":
                draws[drawRecord.curIndex] = new Line();
                break;
            case "rectangle":
                draws[drawRecord.curIndex] = new Rectangle();
                break;
            case "circle":
                draws[drawRecord.curIndex] = new Circle();
                break;
            case "text":
                draws[drawRecord.curIndex] = new Text();
                break;
        }
    }

    public void createDrawForClient(DrawService clientDraw) {
        drawingTool = clientDraw.type;
        ModeSwitcher();

        draws[drawRecord.curIndex].x1 = clientDraw.x1;
        draws[drawRecord.curIndex].y1 = clientDraw.y1;
        draws[drawRecord.curIndex].x2 = clientDraw.x2;
        draws[drawRecord.curIndex].y2 = clientDraw.y2;
        draws[drawRecord.curIndex].type = clientDraw.type;

        if(clientDraw.type.equals("text")) {
            draws[drawRecord.curIndex].s1 = clientDraw.s1;
        }

        drawRecord.nextIndex();
        repaint();
        createDraw();

    }

    public void saveFile() {
        if (saveAs) {
            String[] typeList = new String[] {"jpg", "png", "gif"};
            JList list = new JList(typeList);
            JOptionPane.showMessageDialog(
                    null, list, "Save As?", JOptionPane.PLAIN_MESSAGE);
            String selectedFileType = typeList[list.getSelectedIndex()];
            save(selectedFileType);
        } else {
            save(null);

        }

    }

    public void save(String type) {
        if (saved) {
            File fileName = filePath;
            fileName.canWrite();
            try {
                fileName.delete();
                FileOutputStream fos;
                if (type != null) {
                    fos = new FileOutputStream(fileName + "." + type);
                    SavedFileName = fileName.getName() + "." + type;
                } else {
                    fos = new FileOutputStream(fileName);
                    SavedFileName = fileName.getName();
                }
                output = new ObjectOutputStream(fos);
                output.writeInt(drawRecord.curIndex);
                for (int i = 0; i < drawRecord.curIndex; i++) {
                    DrawService p = draws[i];
                    output.writeObject(p);
                    output.flush();
                }
                output.close();
                fos.close();
            } catch (IOException e) {
                ExceptionHandler.main(e);
            }
        } else {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int result = fileChooser.showSaveDialog(this);
            if (result == JFileChooser.CANCEL_OPTION) {
                return;
            }
            File fileName = fileChooser.getSelectedFile();
            filePath = fileName;
            fileName.canWrite();
            if (fileName == null || fileName.getName().equals("")) {
                JOptionPane.showMessageDialog(fileChooser, "Invalid File Name",
                        "Invalid File Name", JOptionPane.ERROR_MESSAGE);
            } else {
                try {
                    fileName.delete();
                    FileOutputStream fos;
                    if (type != null) {
                        fos = new FileOutputStream(fileName + "." + type);
                        SavedFileName = fileName.getName() + "." + type;
                    } else {
                        fos = new FileOutputStream(fileName);
                        SavedFileName = fileName.getName();
                    }
                    output = new ObjectOutputStream(fos);
                    output.writeInt(drawRecord.curIndex);
                    for (int i = 0; i < drawRecord.curIndex; i++) {
                        DrawService p = draws[i];
                        output.writeObject(p);
                        output.flush();
                    }
                    output.close();
                    fos.close();
                } catch (IOException e) {
                    ExceptionHandler.main(e);
                }
            }
        }

    }

    public void loadFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.CANCEL_OPTION) {
            // do nothing
            return;
        }
        File fileName = fileChooser.getSelectedFile();
        fileName.canRead();
        if (fileName == null || fileName.getName().equals("")) {
            JOptionPane.showMessageDialog(fileChooser, "Can not load without a file name",
                    "Load fails", JOptionPane.ERROR_MESSAGE);
        } else {
            try {
                FileInputStream fis = new FileInputStream(fileName);
                input = new ObjectInputStream(fis);
                DrawService inputRecord;
                int countNumber = 0;
                countNumber = input.readInt();
                for (drawRecord.curIndex = 0; drawRecord.curIndex < countNumber; drawRecord.curIndex++) {
                    inputRecord = (DrawService) input.readObject();
                    draws[drawRecord.curIndex] = inputRecord;
                }
                createDraw();
                input.close();
                repaint();
                CreateWhiteBoard.Broadcast(null, null, "load", null);
            } catch (IOException | ClassNotFoundException e) {
                ExceptionHandler.main(e);
            }
        }
    }

    public void newFile() throws IOException {
        cleanBoard();
        CreateWhiteBoard.Broadcast(null, null, "cleanUp", null);

    }

    public void cleanBoard() {
        saveAs = false;
        saved = false;
        drawRecord.initialiseIndex();
        drawingTool = "pencil";
        createDraw();
        repaint();
    }

    public void exit() {
        System.exit(0);
    }

    public void addNewUser(String user) {
        curUserList.add(user);
        model.addElement(user);
    }

    public void removeUser(String user) {
        curUserList.remove(user);
        model.removeElement(user);
    }
}