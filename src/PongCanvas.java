import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.util.*;

/**
 * Created by Kevin on 8/9/2016.
 */
public class PongCanvas extends Canvas implements Runnable, KeyListener {
    private final boolean DEBUG = false;

    private final int BOX_HEIGHT = 100;
    private final int ELEMENT_WIDTH = 25;
    private final int MOVE_OFFSET = 10;

    private Thread runThread;
    private int leftBoxY = 0;
    private int rightBoxY = 0;
    private Set<Integer> keysPressed = new HashSet<Integer>();

    private boolean twoPlayer = true;
    private int p1Score = 0;
    private int p2score = 0;

    public void update(Graphics g)
    {
        //set up double buffering
        Graphics doubleBufferGraphics;
        BufferedImage doubleBuffer = null;
        Dimension d = this.getSize();
        doubleBuffer = new BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_ARGB);
        doubleBufferGraphics = doubleBuffer.getGraphics();
        doubleBufferGraphics.setColor(this.getBackground());
        doubleBufferGraphics.fillRect(0, 0, d.width, d.height);
        doubleBufferGraphics.setColor(this.getForeground());
        paint(doubleBufferGraphics);


        //flip
        g.drawImage(doubleBuffer, 0, 0, this);
    }

    public void paint(Graphics g)
    {
        Dimension d = this.getSize();
        if (runThread == null) //if runThread = null, then there is no active thread, so init + start game loop
        {
            this.addKeyListener(this);
            leftBoxY = (this.getSize().height / 2) - (BOX_HEIGHT / 2);
            rightBoxY = leftBoxY;
            runThread = new Thread(this);
            runThread.start();
        }

        g.fillRect(0, leftBoxY, ELEMENT_WIDTH, BOX_HEIGHT); //draw left paddle
        g.fillRect(d.width - ELEMENT_WIDTH, rightBoxY, ELEMENT_WIDTH, BOX_HEIGHT); //draw right paddle
        g.drawLine(d.width / 2, 0, d.width / 2, d.height); //draw dividing line
        g.fillOval((d.width / 2) - (ELEMENT_WIDTH / 2), (d.height / 2) - (ELEMENT_WIDTH / 2), ELEMENT_WIDTH, ELEMENT_WIDTH); //draw pong ball
        g.drawString("" + p1Score, d.width / 4, 10); //player 1 score
        g.drawString("" + p2score, (d.width /2) + (d.width / 4), 10); //player 2 score
    }

    @Override
    public void run()
    {
        while(true)
        {
            if (DEBUG)
                System.out.println("Loop");
            for (int k : keysPressed)
            {
                if (DEBUG)
                    System.out.println(k);
                switch (k)
                {
                    case KeyEvent.VK_W:
                        if (!(leftBoxY - MOVE_OFFSET <= 0))
                            leftBoxY -= MOVE_OFFSET;
                        break;
                    case KeyEvent.VK_S:
                        if (!(leftBoxY + BOX_HEIGHT + MOVE_OFFSET >= this.getSize().height))
                            leftBoxY += MOVE_OFFSET;
                        break;
                    case KeyEvent.VK_UP:
                        if (!(rightBoxY - MOVE_OFFSET <= 0))
                            rightBoxY -= MOVE_OFFSET;
                        break;
                    case KeyEvent.VK_DOWN:
                        if (!(rightBoxY + BOX_HEIGHT + MOVE_OFFSET >= this.getSize().height))
                            rightBoxY += MOVE_OFFSET;
                        break;
                }
            }
            repaint();
            try
            {
                Thread.currentThread();
                Thread.sleep(100);
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent e)
    {

    }

    @Override
    public void keyPressed(KeyEvent e)
    {
        if (DEBUG)
            System.out.println("KeyPressed");
        keysPressed.add(e.getKeyCode());

    }

    @Override
    public void keyReleased(KeyEvent e)
    {
        if (DEBUG)
            System.out.println("KeyReleased: " + e.getKeyCode());
        keysPressed.remove(e.getKeyCode());
    }
}
