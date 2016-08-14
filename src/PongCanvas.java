import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.*;

/**
 * Created by Kevin on 8/9/2016.
 */
public class PongCanvas extends Canvas implements Runnable, KeyListener {
    private final boolean DEBUG = false;

    private final int BOX_HEIGHT = 100;
    private final int ELEMENT_WIDTH = 25;
    private final int BOX_SPACE_FROM_WALL = 10;
    private final int MOVE_OFFSET = 10;
    private final int FRAME_DELAY = 50;
    private final int ABS_DEFAULT_DELTA_Y_FULL = 10;
    private final int PADDLE_MOVING_ADDITIONAL_X = 3;
    private final int DEFAULT_DELTA_X = 12;
    private final int START_DELAY_MS = 500;

    Random random = new Random();
    Image menuImage = null;
    private int delayTimer = -FRAME_DELAY; //it is not 0 because the game logic adds FRAME_DELAY to delayTimer before any calls to Thread.sleep()
    private Thread runThread;
    private int leftBoxY = 0;
    private int rightBoxY = 0;
    private int deltaVariation = 1;
    private int p1Score = 0;
    private int p2Score = 0;
    private Point ball;
    private Point deltaBall;
    private Set<Integer> keysPressed = new HashSet<Integer>();
    private boolean twoPlayer = true;
    private boolean moveBall = true;
    private boolean leftMoving = false;
    private boolean rightMoving = false;
    private boolean isInMenu = true;
    private boolean isInEndGame = false;
    private boolean p1Won = false;
    private boolean doResetGame = false;
    private boolean[] noCollide = {false, false, false, false, false, false};

    public void resetGame()
    {
        delayTimer = -FRAME_DELAY;
        Dimension d = this.getSize();
        leftBoxY = (this.getSize().height / 2) - (BOX_HEIGHT / 2);
        rightBoxY = leftBoxY;
        ball = new Point((d.width / 2) - (ELEMENT_WIDTH / 2), (d.height / 2) - (ELEMENT_WIDTH / 2) - 100);
        //determine if the ball should start mvoing to the left or to the right
        boolean startOnLeft = random.nextBoolean();
        if (startOnLeft)
            deltaBall = new Point(-DEFAULT_DELTA_X, ABS_DEFAULT_DELTA_Y_FULL);
        else
            deltaBall = new Point(DEFAULT_DELTA_X, ABS_DEFAULT_DELTA_Y_FULL);
        for (int i = 0; i < CollisionType.TOTAL; i++)
            noCollide[i] = false;
    }

    /**
     * to avoid a bug, speed must be varied, tack this on to any movement of the ball98 to vary it and avoid the bug
     * @return the variation in speed
     */
    public int getDeltaVariation()
    {
        int newVariation = deltaVariation;
        if(deltaVariation == 1)
            deltaVariation = 0;
        else
            deltaVariation = 1;
        return newVariation;
    }

    /**
     * determine the Y position of the ball relative to the paddle it collided with
     * @param boxY send in leftBoxY or rightBoxY depending on the collision
     * @return a new delta Y for the ball
     */
    public int getNewDeltaY(int boxY)
    {
        int middleY = ball.y + (ELEMENT_WIDTH / 2);
        if (middleY < boxY + (BOX_HEIGHT / 2)) //is in top half
        {
            if(DEBUG)
                System.out.println("TOP HALF");
            if (middleY < boxY + (BOX_HEIGHT / 3)) //is in top third
                return -ABS_DEFAULT_DELTA_Y_FULL;
            else //is in top half of middle third
                return -(ABS_DEFAULT_DELTA_Y_FULL / 2);
        }
        else if (middleY > boxY + (BOX_HEIGHT / 2)) //is in bottom half
        {
            if(DEBUG)
                System.out.println("BOTTOM HALF");
            if (middleY > boxY + (BOX_HEIGHT - (BOX_HEIGHT / 3))) //is in bottom third
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
        BufferedImage doubleBuffer;
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
        if (!isInMenu)
        {
            g.fillRect(BOX_SPACE_FROM_WALL, leftBoxY, ELEMENT_WIDTH, BOX_HEIGHT); //draw left paddle
            g.fillRect(d.width - ELEMENT_WIDTH - BOX_SPACE_FROM_WALL, rightBoxY, ELEMENT_WIDTH, BOX_HEIGHT); //draw right paddle
            g.drawLine(d.width / 2, 0, d.width / 2, d.height); //draw dividing line
            g.fillOval(ball.x, ball.y, ELEMENT_WIDTH - 5, ELEMENT_WIDTH - 5); //draw pong ball
            int p1TextX = d.width / 4;
            int p2TextX = (d.width / 2) + (d.width / 4);
            g.drawString("" + p1Score, p1TextX, 10); //player 1 score
            g.drawString("" + p2Score, p2TextX, 10); //player 2 score
            String p1KeyPrompt = "W = Up; S = Down";
            String p2KeyPrompt = "Up Arrow = Up; Down Arrow = Down";
            g.drawString(p1KeyPrompt, p1TextX - (g.getFontMetrics().stringWidth(p1KeyPrompt) / 2), 25); //player 1 key prompt
            if (twoPlayer)
                g.drawString(p2KeyPrompt, p2TextX - (g.getFontMetrics().stringWidth(p2KeyPrompt) / 2), 25); //player 2 key prompt, only draw if 2 player game
            if (isInEndGame)
            {
                if (p1Won)
                {
                    String p1WonPrompt = "Player 1 Wins! Press ENTER to restart.";
                    g.drawString(p1WonPrompt, (d.width / 4) - (g.getFontMetrics().stringWidth(p1WonPrompt) / 2), d.height / 3);
                }
                else
                {
                    String p2WonPrompt = "Player 2 Wins! Press ENTER to restart.";
                    g.drawString(p2WonPrompt, (d.width - (d.width / 4)) - (g.getFontMetrics().stringWidth(p2WonPrompt) / 2), d.height / 3);
                }
            }
        }
        else
        {
            if (menuImage == null)
            {
                try
                {
                    URL imagePath = PongCanvas.class.getResource("Pong-Menu.png");
                    menuImage = Toolkit.getDefaultToolkit().getImage(imagePath);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
            g.drawImage(menuImage, 0, 0, d.width, d.height, this);
        }
    }

    @Override
    public void run()
    {
        while(true)
        {
            Dimension d = this.getSize();
            for (int k : keysPressed) //handle key presses
            {
                if (DEBUG)
                    System.out.println(k);
                switch (k) {
                    case KeyEvent.VK_W: //move left paddle up
                        if (!isInMenu && !isInEndGame) {
                            if (!(leftBoxY - MOVE_OFFSET <= 0)) {
                                leftMoving = true;
                                leftBoxY -= MOVE_OFFSET;
                            } else
                                leftBoxY = 0;
                        }
                        break;
                    case KeyEvent.VK_S: //move left paddle down
                        if (!isInMenu && !isInEndGame) {
                            if (!(leftBoxY + BOX_HEIGHT + MOVE_OFFSET >= d.height)) {
                                leftMoving = true;
                                leftBoxY += MOVE_OFFSET;
                            } else
                                leftBoxY = d.height - BOX_HEIGHT;
                        }
                        break;
                    case KeyEvent.VK_UP: //move right paddle up
                        if (!isInMenu && !isInEndGame) {
                            if (!(rightBoxY - MOVE_OFFSET <= 0) && twoPlayer) {
                                rightMoving = true;
                                rightBoxY -= MOVE_OFFSET;
                            } else
                                rightBoxY = 0;
                        }
                        break;
                    case KeyEvent.VK_DOWN: //move right paddle down
                        if (!isInMenu && !isInEndGame) {
                            if (!(rightBoxY + BOX_HEIGHT + MOVE_OFFSET >= d.height) && twoPlayer) {
                                rightMoving = true;
                                rightBoxY += MOVE_OFFSET;
                            } else
                                rightBoxY = d.height - BOX_HEIGHT;
                        }
                        break;
                    case KeyEvent.VK_1: //start a one player game, TODO: implement one player games
                        break;
                    case KeyEvent.VK_2: //start a two player game
                        if (isInMenu)
                        {
                            isInMenu = false;
                            delayTimer = -FRAME_DELAY;
                        }
                        break;
                    case KeyEvent.VK_ENTER: //restart the game if in end game
                        if (isInEndGame)
                        {
                            isInEndGame = false;
                            p1Score = 0;
                            p2Score = 0;
                            resetGame();
                        }
                        break;
                }
            }
            if (!isInMenu && !isInEndGame)
            {
                if (delayTimer < START_DELAY_MS) //give a delay before the game starts running
                    delayTimer += FRAME_DELAY;
                else
                {
                    //move ball
                    if (moveBall) {
                        int newX = deltaBall.x;
                        int newY = deltaBall.y;
                        if (DEBUG) {
                            System.out.println("x: " + newX);
                            System.out.println("y: " + newY);
                        }
                        ball.x += newX;
                        ball.y += newY;
                    }

                    //check collisions
                    if (ball.x <= ELEMENT_WIDTH + BOX_SPACE_FROM_WALL && ball.y + ELEMENT_WIDTH >= leftBoxY && ball.y <= leftBoxY + BOX_HEIGHT && noCollide[CollisionType.LEFT_PADDLE] == false) //check collision with left box
                    {
                        if (DEBUG)
                            System.out.println("Ball Collide With Left Box");
                        deltaBall.x = DEFAULT_DELTA_X + getDeltaVariation();
                        if (leftMoving)
                            deltaBall.x += PADDLE_MOVING_ADDITIONAL_X;
                        deltaBall.y = getNewDeltaY(leftBoxY) + getDeltaVariation();
                        System.out.println("new X: " + deltaBall.x + " new Y: " + deltaBall.y);
                        //make sure the ball doesn't get stuck by disallowing a double collision, but also reallow all other collisions
                        noCollide[CollisionType.LEFT_PADDLE] = true;
                        noCollide[CollisionType.RIGHT_PADDLE] = false;
                        noCollide[CollisionType.TOP] = false;
                        noCollide[CollisionType.BOTTOM] = false;
                    } else if (ball.x + ELEMENT_WIDTH >= d.width - ELEMENT_WIDTH - BOX_SPACE_FROM_WALL && ball.y + ELEMENT_WIDTH >= rightBoxY && ball.y <= rightBoxY + BOX_HEIGHT && noCollide[CollisionType.RIGHT_PADDLE] == false) //check collision with right box
                    {
                        if (DEBUG)
                            System.out.println("Ball Collide With Right Box");
                        deltaBall.x = -DEFAULT_DELTA_X + getDeltaVariation();
                        if (rightMoving)
                            deltaBall.x -= PADDLE_MOVING_ADDITIONAL_X;
                        deltaBall.y = getNewDeltaY(rightBoxY) + getDeltaVariation();
                        System.out.println("new X: " + deltaBall.x + " new Y: " + deltaBall.y);
                        //make sure the ball doesn't get stuck by disallowing a double collision, but also reallow all other collisions
                        noCollide[CollisionType.LEFT_PADDLE] = false;
                        noCollide[CollisionType.RIGHT_PADDLE] = true;
                        noCollide[CollisionType.TOP] = false;
                        noCollide[CollisionType.BOTTOM] = false;
                    } else if (ball.x <= 0) //player 1 loses
                    {
                        if (DEBUG)
                            System.out.println("Ball Collide With Left Wall");
                        p2Score++;
                        doResetGame = true;
                    } else if (ball.x + ELEMENT_WIDTH >= d.width) //player 2 loses
                    {
                        if (DEBUG)
                            System.out.println("Ball Collide With Right Wall");
                        p1Score++;
                        doResetGame = true;
                    } else if (ball.y <= 0 && noCollide[CollisionType.TOP] == false) //check collisions with top
                    {
                        if (DEBUG)
                            System.out.println("Ball Collide With Top Or Bottom");
                        deltaBall.y = -deltaBall.y + getDeltaVariation();
                        //make sure the ball doesn't get stuck by disallowing a double collision, but also reallow all other collisions
                        noCollide[CollisionType.LEFT_PADDLE] = false;
                        noCollide[CollisionType.RIGHT_PADDLE] = false;
                        noCollide[CollisionType.TOP] = true;
                        noCollide[CollisionType.BOTTOM] = false;
                    } else if (ball.y + ELEMENT_WIDTH >= d.height && noCollide[CollisionType.BOTTOM] == false) //check collision with bottom
                    {
                        if (DEBUG)
                            System.out.println("Ball Collide With Top Or Bottom");
                        deltaBall.y = -deltaBall.y + getDeltaVariation();
                        //make sure the ball doesn't get stuck by disallowing a double collision, but also reallow all other collisions
                        noCollide[CollisionType.LEFT_PADDLE] = false;
                        noCollide[CollisionType.RIGHT_PADDLE] = false;
                        noCollide[CollisionType.TOP] = false;
                        noCollide[CollisionType.BOTTOM] = true;
                    }

                    leftMoving = false;
                    rightMoving = false;

                    //check if any player has ten points
                    if (p1Score >= 10) //player one wins
                    {
                        doResetGame = false;
                        p1Won = true;
                        isInEndGame = true;
                    }
                    if (p2Score >= 10) //player two wins
                    {
                        doResetGame = false;
                        p1Won = false;
                        isInEndGame = true;
                    }

                    //reset the game if needed
                    if (doResetGame)
                        resetGame();
                    doResetGame = false;
                }
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
        leftMoving = false;
        rightMoving = false;
    }
}
