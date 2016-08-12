import java.applet.Applet;
import java.awt.*;

/**
 * Created by Kevin on 8/9/2016.
 */
public class PongApplet extends Applet {

    private PongCanvas c;

    public void init()
    {
        c = new PongCanvas();
        c.setPreferredSize(new Dimension(640, 480));
        c.setVisible(true);
        c.setFocusable(true);
        this.add(c);
        this.setVisible(true);
        this.setSize(new Dimension(640, 480));
    }

    public void paint(Graphics g)
    {
        this.setSize(new Dimension(640,480));
    }
}
