import javax.swing.*;
import java.io.PrintWriter;
import java.io.StringWriter;

public class ExceptionHandler {
    public static void main(Exception e) {
        StringWriter errors = new StringWriter();
        e.printStackTrace(new PrintWriter(errors));
        JOptionPane.showMessageDialog(null, errors.toString());
    }
}
