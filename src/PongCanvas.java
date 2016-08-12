import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.util.*;

/**
 * Created by Kevin on 8/9/2016.
 */
public class PongCanvas extends Canvas implements Runnable, KeyListener {
    private final boolean DEBUG = true;

    private final int BOX_HEIGHT = 100;
    private final int ELEMENT_WIDTH = 25;
    private final int MOVE_OFFSET = 10;
    private final int FRAME_DELAY = 50;
    private final int ABS_DEFAULT_DELTA_Y_FULL = 6;

    private Thread runThread;
    private int leftBoxY = 0;
    private int rightBoxY = 0;
    private Point ball;
    private Point deltaBall;
    private Set<Integer> keysPressed = new HashSet<Integer>();
    private boolean twoPlayer = true;
    private boolean moveBall = true;
    private int p1Score = 0;
    private int p2score = 0;

    public void resetGame()
    {
        Dimension d = this.getSize();
        leftBoxY = (this.getSize().height / 2) - (BOX_HEIGHT / 2);
        rightBoxY = leftBoxY;
        ball = new Point((d.width / 2) - (ELEMENT_WIDTH / 2), (d.height / 2) - (ELEMENT_WIDTH / 2) - 100);
        deltaBall = new Point(-12, 6);
    }

    /**
     * determine the Y position of the ball relative to the paddle it collided with
     * @param boxY send in leftBoxY or rightBoxY depending on the collision
     * @return a new delta Y for the ball
     */
    public int getNewDeltaY (int boxY)
    {
        if (ball.y < boxY + (BOX_HEIGHT / 2)) //is in top half
        {
            if(DEBUG)
                System.out.println("TOP HALF");
            if (ball.y < boxY + (BOX_HEIGHT / 3)) //is in top third
                return -ABS_DEFAULT_DELTA_Y_FULL;
            else //is in top half of middle third
                return -(ABS_DEFAULT_DELTA_Y_FULL / 2);
        }
        else if (ball.y > boxY + (BOX_HEIGHT / 2)) //is in bottom half
        {
            if(DEBUG)
                System.out.println("BOTTOM HALF");
            if (ball.y > boxY + (BOX_HEIGHT - (BOX_HEIGHT / 3))) //is in bottom third
                return ABS_DEFAULT_DELTA_Y_FULL;
            else //is in bottom half of middle third
                return ABS_DEFAULT_DELTA_Y_FULL / 2;
        }
        else //is in exact middle
            return 0;
    }

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
            resetGame();
            runThread = new Thread(this);
            runThread.start();
        }

        g.fillRect(0, leftBoxY, ELEMENT_WIDTH, BOX_HEIGHT); //draw left paddle
        g.fillRect(d.width - ELEMENT_WIDTH, rightBoxY, ELEMENT_WIDTH, BOX_HEIGHT); //draw right paddle
        g.drawLine(d.width / 2, 0, d.width / 2, d.height); //draw dividing line
        g.fillOval(ball.x, ball.y, ELEMENT_WIDTH, ELEMENT_WIDTH); //draw pong ball
        int p1TextX = d.width / 4;
        int p2TextX = (d.width /2) + (d.width / 4);
        g.drawString("" + p1Score, p1TextX, 10); //player 1 score
        g.drawString("" + p2score, p2TextX, 10); //player 2 score
        String p1KeyPrompt = "W = Up; S = Down";
        String p2KeyPrompt = "Up Arrow = Up; Down Arrow = Down";
        g.drawString(p1KeyPrompt, p1TextX - (g.getFontMetrics().stringWidth(p1KeyPrompt) / 2), 25); //player 1 key prompt
        if (twoPlayer)
            g.drawString(p2KeyPrompt, p2TextX - (g.getFontMetrics().stringWidth(p2KeyPrompt) / 2), 25); //player 2 key prompt, only draw if 2 player game
    }

    @Override
    public void run()
    {
        while(true)
        {
            for (int k : keysPressed) //move paddles if keys are pressed
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
                        if (!(rightBoxY - MOVE_OFFSET <= 0) && twoPlayer)
                            rightBoxY -= MOVE_OFFSET;
                        break;
                    case KeyEvent.VK_DOWN:
                        if (!(rightBoxY + BOX_HEIGHT + MOVE_OFFSET >= this.getSize().height) && twoPlayer)
                            rightBoxY += MOVE_OFFSET;
                        break;
                }
            }

            //move ball
            if (moveBall)
            {
                ball.x += deltaBall.x;
                ball.y += deltaBall.y;
            }
            //check collisions
            Dimension d = this.getSize();
            if (ball.x <= 0) //player 1 loses
            {
                if (DEBUG)
                    System.out.println("Ball Collide With Left Wall");
                p2score++;
                resetGame();
            }
            else if (ball.x >= d.width) //player 2 loses
            {
                if (DEBUG)
                    System.out.println("Ball Collide With Right Wall");
                p1Score++;
                resetGame();
            }
            else if (ball.x <= ELEMENT_WIDTH && ball.y >= leftBoxY && ball.y <= leftBoxY + BOX_HEIGHT) //check collision with left box
            {
                if (DEBUG)
                    System.out.println("Ball Collide With Left Box");
                deltaBall.x = -deltaBall.x;
                deltaBall.y = getNewDeltaY(leftBoxY);
            }
            else if (ball.x + ELEMENT_WIDTH >= d.width - ELEMENT_WIDTH && ball.y >= rightBoxY && ball.y <= rightBoxY + BOX_HEIGHT) //check collision with right box
            {
                if (DEBUG)
                    System.out.println("Ball Collide With Right Box");
                deltaBall.x = -deltaBall.x;
                deltaBall.y = getNewDeltaY(rightBoxY);
            }
            else if (ball.y <= 0 || ball.y + ELEMENT_WIDTH >= d.height) //check colisions with top and bottom
            {
                if (DEBUG)
                    System.out.println("Ball Collide With Top Or Bottom");
                deltaBall.y = -deltaBall.y;
            }

            repaint();
            try
            {
                Thread.currentThread();
                Thread.sleep(FRAME_DELAY);
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
