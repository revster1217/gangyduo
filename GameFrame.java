import java.awt.*;
import java.io.*;
import javax.swing.*;

public class GameFrame extends JFrame {
    private GameCanvas canvas;
    private JLabel statusLabel;
    private int playerNumber;
    private Player controlledShip; // Store the ship object

    public GameFrame(Grid sharedArena, DataOutputStream out, int pNum, Player myShip) {
        this.playerNumber = pNum;
        this.controlledShip = myShip; // Initialize it
        
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

    public String getControlledShipName() {
        return controlledShip.getName();
    }

    public void triggerScreenShake() {
        canvas.triggerScreenShake();
    }

    public void setGameActive(boolean active) {
        canvas.setGameActive(active);
    }

    public void showMessage(String m) { statusLabel.setText(m); }
    public int getPlayerNumber() { return playerNumber; }
    public void repaintCanvas() { canvas.repaint(); }

    public void addExplosion(int row, int col) { canvas.addExplosion(row, col); }
    public void addMineExplosion(int row, int col) { canvas.addMineExplosion(row, col); }
}