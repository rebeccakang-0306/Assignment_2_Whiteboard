import java.awt.*;

public class Line extends DrawService {
    void draw(Graphics2D g2d) {
        g2d.setStroke(new BasicStroke(1.0f,
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL));
        g2d.drawLine(x1, y1, x2, y2);
    }
}