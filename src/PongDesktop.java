import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Created by Kevin on 8/9/2016.
 */
public class PongDesktop {
    public static void main(String[] args)
    {
        JFrame frame = new JFrame("Pong");
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        frame.setSize(640, 480);
        PongCanvas c = new PongCanvas();
        frame.add(c);
        frame.setVisible(true);
    }
}
