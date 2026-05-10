import java.awt.*;
import java.io.*;
import javax.swing.*;

/**
 * GameFrame is the main window for each player.
 * 
 * It now holds:
 *   - A status label showing messages like "Your turn!" or "Waiting..."
 *   - Two GameCanvas panels: one for YOUR ocean grid, one for the ENEMY tracking grid
 *   - Whether it is currently this player's turn (used by GameCanvas to allow/block firing)
 *   - A reference to the DataOutputStream so GameCanvas can send FIRE messages
 */
public class GameFrame extends JFrame {

    private GameCanvas canvas;
    private JLabel     statusLabel;
    private boolean    myTurn;
    private int        playerNumber;

    public GameFrame(Player localPlayer, Grid enemyTrackingGrid,
                     DataOutputStream out, int playerNumber) {

        this.playerNumber = playerNumber;
        this.myTurn = false; // server will send YOUR_TURN when it is time

        setTitle("Battleship - Player " + playerNumber);
        setSize(1024, 768);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // --- Status bar at the top ---
        statusLabel = new JLabel("Connecting...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 16));
        statusLabel.setOpaque(true);
        statusLabel.setBackground(new Color(30, 30, 60));
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setPreferredSize(new Dimension(1024, 40));
        add(statusLabel, BorderLayout.NORTH);

        // --- Canvas: draws BOTH grids side by side ---
        canvas = new GameCanvas(localPlayer, enemyTrackingGrid, out, this);
        add(canvas, BorderLayout.CENTER);
    }

    /** Called by NetworkThread (via SwingUtilities.invokeLater) to update the status bar. */
    public void showMessage(String message) {
        statusLabel.setText(message);
    }

    /** Called by NetworkThread to toggle whether this player can fire. */
    public void setMyTurn(boolean myTurn) {
        this.myTurn = myTurn;
    }

    /** GameCanvas checks this before sending a FIRE message. */
    public boolean isMyTurn() {
        return myTurn;
    }

    /** Triggers a repaint of the canvas. */
    public void repaintCanvas() {
        canvas.repaint();
    }

    public int getPlayerNumber() {
        return playerNumber;
    }
}
