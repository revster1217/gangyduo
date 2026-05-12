import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;

public class GameCanvas extends JComponent {
    private Grid sharedArena;
    private DataOutputStream out;
    private Ship controlledShip;
    private boolean gameActive = false;

    private final int TILE_SIZE = 35; // Down from 45
    private final int OFFSET_X = 135; 
    private final int OFFSET_Y = 60;

    // Variables to track the cooldowns
    private long lastFireTime = 0;
    private long lastMoveTime = 0;
    private long lastSmokeTime = 0;

    public GameCanvas(Grid arena, DataOutputStream o, Ship myShip) {
        this.sharedArena = arena;
        this.out = o;
        this.controlledShip = myShip;

        setupKeyBindings();

        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!gameActive) return;

                // Check if 1 second (1000 ms) has passed since the last shot
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastFireTime < 1000) {
                    return; // Ignore the click if the cooldown is still active
                }

                int col = (e.getX() - OFFSET_X) / TILE_SIZE;
                int row = (e.getY() - OFFSET_Y) / TILE_SIZE;

                if (row >= 0 && row < 10 && col >= 0 && col < 10) {
                    try {
                        out.writeUTF("FIRE:" + row + ":" + col);
                        out.flush();
                        lastFireTime = currentTime; // Update the fire timestamp on success
                    } catch (IOException ex) {}
                }
            }
        });
    }

    public void setGameActive(boolean active) { this.gameActive = active; }

    private void setupKeyBindings() {
        InputMap im = this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = this.getActionMap();

        im.put(KeyStroke.getKeyStroke("UP"), "U");
        im.put(KeyStroke.getKeyStroke("DOWN"), "D");
        im.put(KeyStroke.getKeyStroke("LEFT"), "L");
        im.put(KeyStroke.getKeyStroke("RIGHT"), "R");
        im.put(KeyStroke.getKeyStroke("SPACE"), "S");
        im.put(KeyStroke.getKeyStroke("X"), "X"); // NEW

        am.put("U", new AbstractAction() { public void actionPerformed(ActionEvent e) { requestMove(-1, 0, controlledShip.isHorizontal()); }});
        am.put("D", new AbstractAction() { public void actionPerformed(ActionEvent e) { requestMove(1, 0, controlledShip.isHorizontal()); }});
        am.put("L", new AbstractAction() { public void actionPerformed(ActionEvent e) { requestMove(0, -1, controlledShip.isHorizontal()); }});
        am.put("R", new AbstractAction() { public void actionPerformed(ActionEvent e) { requestMove(0, 1, controlledShip.isHorizontal()); }});
        am.put("S", new AbstractAction() { public void actionPerformed(ActionEvent e) { requestMove(0, 0, !controlledShip.isHorizontal()); }});
        am.put("X", new AbstractAction() { public void actionPerformed(ActionEvent e) { 
            if (!gameActive) return;
            long currentTime = System.currentTimeMillis();
            // 15-second cooldown for Smoke Screen
            if (currentTime - lastSmokeTime < 15000) return; 
            try {
                out.writeUTF("ABILITY:SMOKE:" + controlledShip.getName());
                out.flush();
                lastSmokeTime = currentTime;
            } catch (IOException ex) {}
        }
    });
    }

    private void requestMove(int dR, int dC, boolean h) {
        if (!gameActive) return;

        // Check if 1 second (1000 ms) has passed since the last movement
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMoveTime < 1000) {
            return; // Ignore the key press if the cooldown is still active
        }

        int nR = controlledShip.getStartRow() + dR;
        int nC = controlledShip.getStartCol() + dC;
        try {
            // Ask server for permission. Do NOT move it locally yet!
            out.writeUTF("REQUEST_MOVE:" + controlledShip.getName() + ":" + nR + ":" + nC + ":" + (h ? "H" : "V"));
            out.flush();
            lastMoveTime = currentTime; // Update the move timestamp on success
        } catch (IOException ex) {}
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        
        g2d.setColor(Color.BLACK);
        g2d.drawString("BATTLE ARENA (Press 'X' for Smoke Screen)", OFFSET_X, OFFSET_Y - 10);

        for (int r = 0; r < Grid.SIZE; r++) {
            for (int c = 0; c < Grid.SIZE; c++) {
                int s = sharedArena.getTileStatus(r, c);

                // --- NEW INVISIBILITY CHECK ---
                boolean hideTile = false;
                for (Ship ship : sharedArena.getShips()) {
                    // If this is the ENEMY ship, and it is SMOKED, and it occupies this tile: HIDE IT
                    if (!ship.getName().equals(controlledShip.getName()) && ship.isSmoked()) {
                        if (ship.occupies(r, c)) {
                            hideTile = true;
                            break;
                        }
                    }
                }

                // Apply colors based on visibility
                if (hideTile) {
                    g2d.setColor(new Color(0, 119, 190)); // Disguise as WATER
                } else if (s == Grid.WATER) {
                    g2d.setColor(new Color(0, 119, 190));
                } else if (s == Grid.SHIP) {
                    g2d.setColor(Color.DARK_GRAY);
                } else if (s == Grid.MISS) {
                    g2d.setColor(Color.WHITE);
                } else if (s == Grid.HIT) {
                    g2d.setColor(Color.RED);
                } else if (s == Grid.ISLAND) {
                    g2d.setColor(new Color(34, 139, 34));
                } else if (s == Grid.MINE) {
                    g2d.setColor(Color.ORANGE); 
                }

                int x = OFFSET_X + (c * TILE_SIZE);
                int y = OFFSET_Y + (r * TILE_SIZE);
                g2d.fillRect(x, y, TILE_SIZE, TILE_SIZE);
                g2d.setColor(Color.BLACK);
                g2d.drawRect(x, y, TILE_SIZE, TILE_SIZE);
            }
        }
        
        // Highlight your own ship (Make it Gray if YOU are smoked, Green if normal)
        if (controlledShip.isSmoked()) {
            g2d.setColor(Color.LIGHT_GRAY); 
            g2d.setStroke(new BasicStroke(3)); // Thicker border to show it's active
        } else {
            g2d.setColor(Color.GREEN);
            g2d.setStroke(new BasicStroke(1));
        }

        int shipWidth = controlledShip.isHorizontal() ? TILE_SIZE * controlledShip.getLength() : TILE_SIZE;
        int shipHeight = controlledShip.isHorizontal() ? TILE_SIZE : TILE_SIZE * controlledShip.getLength();
        
        g2d.drawRect(OFFSET_X + (controlledShip.getStartCol() * TILE_SIZE), 
                     OFFSET_Y + (controlledShip.getStartRow() * TILE_SIZE), 
                     shipWidth, shipHeight);
        
        g2d.setStroke(new BasicStroke(1)); // Reset stroke for other drawings
    }
}