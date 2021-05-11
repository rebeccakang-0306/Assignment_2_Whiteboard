import java.net.Socket;
import java.util.ArrayList;



public class User {
    public String username;
    public Socket userSocket;
    public String role;

    public static ArrayList<User> curUserList = new ArrayList<>();

    public User(String name, Socket socket, String role) {
        this.username = name;
        this.userSocket = socket;
        this.role = role;
    }

    public static void addInUserList(User newUser) {
        curUserList.add(newUser);
    }

    public static ArrayList<User> getUserList() {
        return curUserList;
    }

    public static void removeFromUserList(int index) {
        curUserList.remove(index);
    }

    public static ArrayList<String> UsernameList() {
        ArrayList<String> usernames = new ArrayList<>();
        for(User eachUser: curUserList) {
            usernames.add(eachUser.username);
        }

        return  usernames;
    }

}
