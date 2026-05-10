import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;

/**
 * GameCanvas draws two grids side by side:
 *   LEFT  — Your own ocean grid (your ships, opponent's hits/misses on you)
 *   RIGHT — Enemy tracking grid (where YOU have fired; hits/misses shown)
 * 
 * Mouse clicks on the RIGHT grid send a FIRE message to the server,
 * but only if it is currently this player's turn.
 * 
 * Key bindings are removed here since ship placement is now fixed at start.
 * (A placement phase can be added later.)
 */
public class GameCanvas extends JComponent {

    // --- Layout constants ---
    private static final int TILE_SIZE   = 40;
    private static final int OFFSET_Y    = 80;   // top padding
    private static final int LEFT_X      = 50;   // x-start of YOUR grid (needs room for row labels)
    private static final int RIGHT_X     = 550;  // x-start of ENEMY grid (LEFT_X + 10*TILE + gap)
    private static final int LABEL_Y     = 60;   // y for grid title labels

    // --- Game data ---
    private Player          localPlayer;       // your ships & your ocean grid
    private Grid            enemyTrackingGrid; // where you have fired
    private DataOutputStream out;             // stream to send FIRE messages to server
    private GameFrame       gameFrame;         // to check isMyTurn() and show messages

    // Selected ammo type (can be toggled via buttons later)
    private String selectedAmmo = "NORMAL";

    public GameCanvas(Player localPlayer, Grid enemyTrackingGrid,
                      DataOutputStream out, GameFrame gameFrame) {
        this.localPlayer       = localPlayer;
        this.enemyTrackingGrid = enemyTrackingGrid;
        this.out               = out;
        this.gameFrame         = gameFrame;

        setPreferredSize(new Dimension(1024, 768));
        setupAmmoButtons();
        setupMouseListener();
    }

    // -------------------------------------------------------------------------
    //  MOUSE LISTENER — only fires on the RIGHT (enemy) grid
    // -------------------------------------------------------------------------
    private void setupMouseListener() {
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Only allow firing on this player's turn
                if (!gameFrame.isMyTurn()) {
                    gameFrame.showMessage("Not your turn! Wait for the opponent.");
                    return;
                }

                int clickX = e.getX() - RIGHT_X;
                int clickY = e.getY() - OFFSET_Y;

                if (clickX >= 0 && clickY >= 0) {
                    int col = clickX / TILE_SIZE;
                    int row = clickY / TILE_SIZE;

                    if (row < Grid.SIZE && col < Grid.SIZE) {
                        // Check if this cell was already attacked
                        int status = enemyTrackingGrid.getTileStatus(row, col);
                        if (status == Grid.HIT || status == Grid.MISS) {
                            gameFrame.showMessage("Already fired there! Choose another cell.");
                            return;
                        }

                        // Send FIRE message to server
                        sendFireMessage(selectedAmmo, row, col);

                        // Temporarily disable further firing until server responds
                        gameFrame.setMyTurn(false);
                        gameFrame.showMessage("Fired " + selectedAmmo + " at ["
                                + row + "][" + col + "]. Waiting for result...");
                    }
                }
            }
        });
    }

    // -------------------------------------------------------------------------
    //  AMMO SELECTION BUTTONS (inner anonymous ActionListeners)
    // -------------------------------------------------------------------------
    private void setupAmmoButtons() {
        // We use key bindings to switch ammo types
        InputMap  im = this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = this.getActionMap();

        // Press 1 = Normal, 2 = Area, 3 = Radar
        im.put(KeyStroke.getKeyStroke("1"), "normalAmmo");
        im.put(KeyStroke.getKeyStroke("2"), "areaAmmo");
        im.put(KeyStroke.getKeyStroke("3"), "radarAmmo");

        am.put("normalAmmo", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                selectedAmmo = "NORMAL";
                gameFrame.showMessage("Ammo selected: Normal Shell (1 tile)");
            }
        });
        am.put("areaAmmo", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (localPlayer.hasAreaAmmo()) {
                    selectedAmmo = "AREA";
                    gameFrame.showMessage("Ammo selected: Area Clear Shell (3x3)");
                } else {
                    gameFrame.showMessage("No Area ammo left!");
                }
            }
        });
        am.put("radarAmmo", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (localPlayer.hasRadarAmmo()) {
                    selectedAmmo = "RADAR";
                    gameFrame.showMessage("Ammo selected: Radar Ping (reveals 3x3 area)");
                } else {
                    gameFrame.showMessage("No Radar ammo left!");
                }
            }
        });
    }

    // -------------------------------------------------------------------------
    //  SEND FIRE MESSAGE to server
    // -------------------------------------------------------------------------
    private void sendFireMessage(String ammoType, int row, int col) {
        try {
            String message = "FIRE:" + ammoType + ":" + row + ":" + col;
            out.writeUTF(message);
            out.flush();
            System.out.println("Sent: " + message);

            // Deduct ammo locally
            if (ammoType.equals("AREA"))  localPlayer.useAreaAmmo();
            if (ammoType.equals("RADAR")) localPlayer.useRadarAmmo();

            // After firing, revert to normal ammo automatically
            selectedAmmo = "NORMAL";

        } catch (IOException e) {
            System.out.println("Failed to send fire message: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------------------
    //  PAINT COMPONENT — draws both grids
    // -------------------------------------------------------------------------
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Background
        g2d.setColor(new Color(15, 15, 40));
        g2d.fillRect(0, 0, getWidth(), getHeight());

        // Draw grid titles
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.setColor(Color.WHITE);
        g2d.drawString("YOUR FLEET", LEFT_X + (Grid.SIZE * TILE_SIZE / 2) - 40, LABEL_Y);
        g2d.drawString("ENEMY WATERS", RIGHT_X + (Grid.SIZE * TILE_SIZE / 2) - 50, LABEL_Y);

        // Draw turn indicator — below both grids
        int infoY = OFFSET_Y + Grid.SIZE * TILE_SIZE + 30;
        g2d.setFont(new Font("Arial", Font.BOLD, 14));
        g2d.setColor(gameFrame.isMyTurn() ? Color.GREEN : Color.RED);
        g2d.drawString(gameFrame.isMyTurn() ? "[ YOUR TURN — Click enemy grid to fire ]"
                                            : "[ OPPONENT'S TURN — Please wait ]",
                LEFT_X, infoY);

        // Draw ammo info below the turn indicator
        g2d.setColor(Color.CYAN);
        g2d.setFont(new Font("Arial", Font.PLAIN, 12));
        g2d.drawString("Selected ammo: " + selectedAmmo
                + "   |   Radar left: " + localPlayer.getRadarAmmoCount()
                + "   |   Area left: "  + localPlayer.getAreaAmmoCount()
                + "   |   Keys: 1=Normal  2=Area  3=Radar",
                LEFT_X, infoY + 22);

        // --- LEFT GRID: Your ocean (shows your ships + opponent hits/misses) ---
        drawGrid(g2d, localPlayer.getOceanGrid(), LEFT_X, true);

        // --- RIGHT GRID: Enemy tracking (shows where YOU fired) ---
        drawGrid(g2d, enemyTrackingGrid, RIGHT_X, false);
    }

    /**
     * Draws one grid (10x10).
     * @param showShips  true = draw ships (your fleet). false = hide ships (enemy waters).
     */
    private void drawGrid(Graphics2D g2d, Grid grid, int startX, boolean showShips) {

        // LAYER 1: Water base
        for (int row = 0; row < Grid.SIZE; row++) {
            for (int col = 0; col < Grid.SIZE; col++) {
                int x = startX + (col * TILE_SIZE);
                int y = OFFSET_Y + (row * TILE_SIZE);

                g2d.setColor(new Color(0, 119, 190));
                g2d.fillRect(x, y, TILE_SIZE, TILE_SIZE);
                g2d.setColor(Color.BLACK);
                g2d.drawRect(x, y, TILE_SIZE, TILE_SIZE);
            }
        }

        // LAYER 2: Ships (only on your own grid)
        if (showShips) {
            for (Ship ship : grid.getShips()) {
                if (ship.isPlaced()) {
                    g2d.setColor(Color.DARK_GRAY);
                    int sx = startX + (ship.getStartCol() * TILE_SIZE);
                    int sy = OFFSET_Y + (ship.getStartRow() * TILE_SIZE);
                    int sw = ship.isHorizontal() ? (ship.getLength() * TILE_SIZE) : TILE_SIZE;
                    int sh = ship.isHorizontal() ? TILE_SIZE : (ship.getLength() * TILE_SIZE);
                    g2d.fillRect(sx, sy, sw, sh);
                    g2d.setColor(Color.BLACK);
                    g2d.drawRect(sx, sy, sw, sh);
                }
            }
        }

        // LAYER 3: Hits and Misses
        for (int row = 0; row < Grid.SIZE; row++) {
            for (int col = 0; col < Grid.SIZE; col++) {
                int status = grid.getTileStatus(row, col);
                int x = startX + (col * TILE_SIZE);
                int y = OFFSET_Y + (row * TILE_SIZE);

                if (status == Grid.HIT) {
                    g2d.setColor(Color.RED);
                    g2d.fillRect(x, y, TILE_SIZE, TILE_SIZE);
                    g2d.setColor(Color.BLACK);
                    g2d.drawRect(x, y, TILE_SIZE, TILE_SIZE);
                    // Draw X mark
                    g2d.setColor(Color.WHITE);
                    g2d.drawLine(x + 5, y + 5, x + TILE_SIZE - 5, y + TILE_SIZE - 5);
                    g2d.drawLine(x + TILE_SIZE - 5, y + 5, x + 5, y + TILE_SIZE - 5);

                } else if (status == Grid.MISS) {
                    g2d.setColor(Color.WHITE);
                    g2d.fillRect(x, y, TILE_SIZE, TILE_SIZE);
                    g2d.setColor(Color.BLACK);
                    g2d.drawRect(x, y, TILE_SIZE, TILE_SIZE);
                    // Draw dot
                    g2d.setColor(new Color(0, 119, 190));
                    g2d.fillOval(x + 10, y + 10, TILE_SIZE - 20, TILE_SIZE - 20);
                }
            }
        }

        // LAYER 4: Row and column labels (A-J, 1-10)
        g2d.setFont(new Font("Arial", Font.PLAIN, 11));
        g2d.setColor(Color.WHITE);
        for (int i = 0; i < Grid.SIZE; i++) {
            // Column numbers across the top
            g2d.drawString(String.valueOf(i + 1),
                    startX + (i * TILE_SIZE) + TILE_SIZE / 2 - 4,
                    OFFSET_Y - 5);
            // Row letters down the left side
            g2d.drawString(String.valueOf((char) ('A' + i)),
                    startX - 18,
                    OFFSET_Y + (i * TILE_SIZE) + TILE_SIZE / 2 + 5);
        }
    }
}
