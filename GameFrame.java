import java.awt.*;
import java.io.*;
import javax.swing.*;

public class GameFrame extends JFrame {
    private GameCanvas canvas;
    private JLabel statusLabel;
    private int playerNumber;
    private boolean myTurn = false; // FIX: Declared the missing myTurn variable

    public GameFrame(Grid sharedArena, DataOutputStream out, int pNum, Ship myShip) {
        this.playerNumber = pNum;
        setTitle("Real-Time Battleship - Player " + pNum);
        setSize(800, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        statusLabel = new JLabel("Connecting...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 18));
        statusLabel.setOpaque(true);
        statusLabel.setBackground(Color.DARK_GRAY);
        statusLabel.setForeground(Color.CYAN);
        statusLabel.setPreferredSize(new Dimension(800, 50));
        add(statusLabel, BorderLayout.NORTH);

        canvas = new GameCanvas(sharedArena, out, myShip);
        add(canvas, BorderLayout.CENTER);
    }

    /** Called by NetworkThread to toggle whether this player can fire. */
    public void setMyTurn(boolean myTurn) {
        this.myTurn = myTurn;
    }

    /** GameCanvas checks this before sending a FIRE message. */
    public boolean isMyTurn() {
        return myTurn;
    }

    public void setGameActive(boolean active) {
        canvas.setGameActive(active);
    }

    public void showMessage(String m) { statusLabel.setText(m); }
    public int getPlayerNumber() { return playerNumber; }
    public void repaintCanvas() { canvas.repaint(); }
}