import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import javax.swing.*;

public class GameCanvas extends JComponent {
    private Grid sharedArena;
    private DataOutputStream out;
    private Ship controlledShip;
    private boolean gameActive = false;

    private final int TILE_SIZE = 35; 
    private final int OFFSET_X = 135; 
    private final int OFFSET_Y = 60;

    private long lastFireTime = 0;
    private long lastMoveTime = 0;
    private long lastSmokeTime = 0;

    // Shake and Wave variables
    private int shakeX = 0;
    private int shakeY = 0;
    private Timer shakeTimer;
    private float wavePhase = 0f; 

    private ArrayList<Explosion> activeExplosions = new ArrayList<>();
    private Timer animationTimer; 
    private static final int EXPLOSION_FRAMES      = 7;  
    private static final int MINE_EXPLOSION_FRAMES = 10;
    private long lastSpecialTime = 0; // Cooldown for special ammo
  

    private class Explosion {
        int row, col;
        int frame;
        boolean isMine;

        Explosion(int row, int col, boolean isMine) {
            this.row = row; this.col = col; this.frame = 0; this.isMine = isMine;
        }

        boolean isFinished() { return frame >= (isMine ? MINE_EXPLOSION_FRAMES : EXPLOSION_FRAMES); }

        void draw(Graphics2D g2d, int curX, int curY) {
            int x  = curX + (col * TILE_SIZE);
            int y  = curY + (row * TILE_SIZE);
            int cx = x + TILE_SIZE / 2;
            int cy = y + TILE_SIZE / 2;

            if (isMine) {
                int[] sizes = {8, 14, 20, 28, 34, 30, 26, 20, 14, 7};
                int size = sizes[Math.min(frame, sizes.length - 1)];

                if (frame >= 2 && frame <= 6) {
                    int ring = size + 8;
                    g2d.setColor(new Color(255, 200, 0, 120));
                    g2d.fillOval(cx - ring, cy - ring, ring * 2, ring * 2);
                }

                if (frame <= 3) {
                    g2d.setColor(new Color(255, 90, 0));
                    g2d.fillOval(cx - size, cy - size, size * 2, size * 2);
                    int inner = size / 2;
                    g2d.setColor(new Color(255, 240, 100));
                    g2d.fillOval(cx - inner, cy - inner, inner * 2, inner * 2);
                } else if (frame <= 6) {
                    g2d.setColor(new Color(200, 30, 0));
                    g2d.fillOval(cx - size, cy - size, size * 2, size * 2);
                    int inner = size / 2;
                    g2d.setColor(Color.WHITE);
                    g2d.fillOval(cx - inner, cy - inner, inner * 2, inner * 2);
                } else {
                    g2d.setColor(new Color(60, 60, 60, 160));
                    g2d.fillOval(cx - size, cy - size, size * 2, size * 2);
                }
            } else {
                int[] sizes = {6, 12, 18, 24, 22, 16, 8};
                int size = sizes[Math.min(frame, sizes.length - 1)];

                if (frame <= 2) {
                    g2d.setColor(new Color(255, 100, 0));
                    g2d.fillOval(cx - size, cy - size, size * 2, size * 2);
                    int inner = size / 2;
                    g2d.setColor(new Color(255, 230, 0));
                    g2d.fillOval(cx - inner, cy - inner, inner * 2, inner * 2);
                } else if (frame <= 4) {
                    g2d.setColor(new Color(255, 80, 0));
                    g2d.fillOval(cx - size, cy - size, size * 2, size * 2);
                    int inner = size / 2;
                    g2d.setColor(Color.WHITE);
                    g2d.fillOval(cx - inner, cy - inner, inner * 2, inner * 2);
                } else {
                    g2d.setColor(new Color(80, 80, 80, 180));
                    g2d.fillOval(cx - size, cy - size, size * 2, size * 2);
                }
            }
        }
    }

    public GameCanvas(Grid arena, DataOutputStream o, Ship myShip) {
        this.sharedArena = arena;
        this.out = o;
        this.controlledShip = myShip;

        setupKeyBindings();

        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!gameActive) return;
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastFireTime < 200) return; 

                int col = (e.getX() - (OFFSET_X + shakeX)) / TILE_SIZE;
                int row = (e.getY() - (OFFSET_Y + shakeY)) / TILE_SIZE;

                if (row >= 0 && row < Grid.SIZE && col >= 0 && col < Grid.SIZE) {
                    try {
                        out.writeUTF("FIRE:" + row + ":" + col);
                        out.flush();
                        lastFireTime = currentTime; 
                    } catch (IOException ex) {}
                }
            }
        });

        animationTimer = new Timer(50, e -> {
            wavePhase += 0.05f;
            for (int i = activeExplosions.size() - 1; i >= 0; i--) {
                Explosion exp = activeExplosions.get(i);
                exp.frame++;
                if (exp.isFinished()) activeExplosions.remove(i);
            }
            repaint(); 
        });
        animationTimer.start();
    }

    // Helper to draw the triangle ship segments
    private void drawShipSegment(Graphics2D g2d, int r, int c, int segmentIndex, int totalLength, boolean isHorizontal, int curX, int curY) {
        int x = curX + (c * TILE_SIZE);
        int y = curY + (r * TILE_SIZE);

        if (segmentIndex == 0) {
            // BOW (Triangle)
            Polygon nose = new Polygon();
            if (isHorizontal) {
                nose.addPoint(x + TILE_SIZE, y); nose.addPoint(x + TILE_SIZE, y + TILE_SIZE);
                nose.addPoint(x, y + TILE_SIZE / 2);
            } else {
                nose.addPoint(x, y + TILE_SIZE); nose.addPoint(x + TILE_SIZE, y + TILE_SIZE);
                nose.addPoint(x + TILE_SIZE / 2, y);
            }
            g2d.fillPolygon(nose);
        } else if (segmentIndex == totalLength - 1) {
            // STERN (Triangle)
            Polygon tail = new Polygon();
            if (isHorizontal) {
                tail.addPoint(x, y); tail.addPoint(x, y + TILE_SIZE);
                tail.addPoint(x + TILE_SIZE, y + TILE_SIZE / 2);
            } else {
                tail.addPoint(x, y); tail.addPoint(x + TILE_SIZE, y);
                tail.addPoint(x + TILE_SIZE / 2, y + TILE_SIZE);
            }
            g2d.fillPolygon(tail);
        } else {
            // MIDDLE (Rectangle)
            g2d.fillRect(x, y, TILE_SIZE, TILE_SIZE);
        }
    }

    public void triggerScreenShake() {
        if (shakeTimer != null && shakeTimer.isRunning()) return;
        final long startTime = System.currentTimeMillis();
        shakeTimer = new Timer(30, e -> {
            if (System.currentTimeMillis() - startTime < 300) {
                shakeX = (int) (Math.random() * 11) - 5;
                shakeY = (int) (Math.random() * 11) - 5;
            } else {
                shakeX = 0; shakeY = 0; shakeTimer.stop();
            }
        });
        shakeTimer.start();
    }

    public void setGameActive(boolean active) { this.gameActive = active; }
    public void addExplosion(int row, int col) { activeExplosions.add(new Explosion(row, col, false)); }
    public void addMineExplosion(int row, int col) { activeExplosions.add(new Explosion(row, col, true)); }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        int curX = OFFSET_X + shakeX;
        int curY = OFFSET_Y + shakeY;

        // 1. BACKGROUND TIDE
        g2d.setColor(new Color(5, 20, 40)); 
        g2d.fillRect(0, 0, getWidth(), getHeight());

    // Draw 5 overlapping, moving wave layers
         for (int i = 0; i < 5; i++) {
        // Each layer gets slightly lighter and more opaque
        g2d.setColor(new Color(0, 60 + (i * 20), 120 + (i * 25), 90)); 
        Polygon wave = new Polygon();
        
        wave.addPoint(0, getHeight()); // Start at bottom left
        
        float frequency = 0.005f + (i * 0.002f); 
        float speed = 0.8f + (i * 0.2f);
        int amplitude = 30 + (i * 5);
        int yOffset = (i * (getHeight() / 5)); // Spacing layers vertically
        
        for (int x = 0; x <= getWidth() + 50; x += 20) {
            // The Math.sin logic that creates the wave shape
            int y = (int) (yOffset + Math.sin((x * frequency) + (wavePhase * speed)) * amplitude);
            wave.addPoint(x, y);
        }
        
        wave.addPoint(getWidth(), getHeight()); // End at bottom right
        g2d.fillPolygon(wave);
        }

        // 2. GRID TILES
        for (int r = 0; r < Grid.SIZE; r++) {
            for (int c = 0; c < Grid.SIZE; c++) {
                int s = sharedArena.getTileStatus(r, c);
                int x = curX + (c * TILE_SIZE);
                int y = curY + (r * TILE_SIZE);

                // Smoke logic
                boolean hideTile = false;
                for (Ship ship : sharedArena.getShips()) {
                    if (!ship.getName().equals(controlledShip.getName()) && ship.isSmoked()) {
                        if (ship.occupies(r, c)) { hideTile = true; break; }
                    }
                }

                if (hideTile || s == Grid.WATER) {
                    g2d.setColor(new Color(0, 119, 190, 90));
                    g2d.fillRect(x, y, TILE_SIZE, TILE_SIZE);
                } else if (s == Grid.ISLAND) {
                    g2d.setColor(new Color(34, 139, 34));
                    g2d.fillRect(x, y, TILE_SIZE, TILE_SIZE);
                } else if (s == Grid.MINE) {
                    g2d.setColor(Color.ORANGE);
                    g2d.fillOval(x + 5, y + 5, TILE_SIZE - 10, TILE_SIZE - 10);
                } else if (s == Grid.SHIP || s == Grid.HIT) {
                    for (Ship ship : sharedArena.getShips()) {
                        if (ship.occupies(r, c)) {
                            int index = ship.isHorizontal() ? (c - ship.getStartCol()) : (r - ship.getStartRow());
                            g2d.setColor(s == Grid.HIT ? Color.RED : Color.DARK_GRAY);
                            drawShipSegment(g2d, r, c, index, ship.getLength(), ship.isHorizontal(), curX, curY);
                            break;
                        }
                    }
                } else if (s == Grid.MISS) {
                    g2d.setColor(new Color(255, 255, 255, 120));
                    g2d.fillOval(x + 12, y + 12, TILE_SIZE - 24, TILE_SIZE - 24);
                }
                g2d.setColor(new Color(255, 255, 255, 30));
                g2d.drawRect(x, y, TILE_SIZE, TILE_SIZE);
            }
        }
        
        // 3. EXPLOSIONS
        try {
            for (Explosion exp : activeExplosions) exp.draw(g2d, curX, curY);
        } catch (Exception ex) {}
    }

    private void setupKeyBindings() {
        InputMap im = this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = this.getActionMap();

        im.put(KeyStroke.getKeyStroke("UP"), "U");
        im.put(KeyStroke.getKeyStroke("DOWN"), "D");
        im.put(KeyStroke.getKeyStroke("LEFT"), "L");
        im.put(KeyStroke.getKeyStroke("RIGHT"), "R");
        im.put(KeyStroke.getKeyStroke("SPACE"), "S");
        im.put(KeyStroke.getKeyStroke("X"), "X");
        im.put(KeyStroke.getKeyStroke("C"), "C");

        am.put("U", new AbstractAction() { 
            public void actionPerformed(ActionEvent e) { requestMove(-1, 0, controlledShip.isHorizontal()); }
        });
        am.put("D", new AbstractAction() { 
            public void actionPerformed(ActionEvent e) { requestMove(1, 0, controlledShip.isHorizontal()); }
        });
        am.put("L", new AbstractAction() { 
            public void actionPerformed(ActionEvent e) { requestMove(0, -1, controlledShip.isHorizontal()); }
        });
        am.put("R", new AbstractAction() { 
            public void actionPerformed(ActionEvent e) { requestMove(0, 1, controlledShip.isHorizontal()); }
        });
        
        am.put("S", new AbstractAction() { 
            public void actionPerformed(ActionEvent e) { requestMove(0, 0, !controlledShip.isHorizontal()); }
        });
        
        am.put("X", new AbstractAction() { 
            public void actionPerformed(ActionEvent e) { 
                if (!gameActive) return;
                
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastSmokeTime < 15000) return; 
                
                try {
                    out.writeUTF("ABILITY:SMOKE:" + controlledShip.getName());
                    out.flush();
                    lastSmokeTime = currentTime; 
                } catch (IOException ex) {
                    System.out.println("Error sending smoke ability.");
                }
            }
        });

        am.put("C", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                if (!gameActive) return;
                long currentTime = System.currentTimeMillis();
                
                // 20-second cooldown for special ammo
                if (currentTime - lastSpecialTime < 20000) return;

                try {
                    out.writeUTF("ABILITY:CLUSTER_BOMB");
                    out.flush();
                    lastSpecialTime = currentTime;
                } catch (IOException ex) {
                    System.out.println("Error sending special ammo request.");
                }
            }
        });
    }

    private void requestMove(int dR, int dC, boolean h) {
        if (!gameActive) return;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastMoveTime < 1000) return; 

        int nR = controlledShip.getStartRow() + dR;
        int nC = controlledShip.getStartCol() + dC;
        
        try {
            out.writeUTF("REQUEST_MOVE:" + controlledShip.getName() + ":" + nR + ":" + nC + ":" + (h ? "H" : "V"));
            out.flush();
            lastMoveTime = currentTime; 
        } catch (IOException ex) {
            System.out.println("Error sending move request.");
        }
    }
}