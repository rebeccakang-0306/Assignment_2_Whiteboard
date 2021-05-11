import java.awt.*;

public class Rectangle extends DrawService {
    void draw(Graphics2D g2d) {
        g2d.setStroke(new BasicStroke(1.0f));
        g2d.drawRect(Math.min(x1, x2), Math.min(y1, y2),
                Math.abs(x1 - x2), Math.abs(y1 - y2));
    }
}