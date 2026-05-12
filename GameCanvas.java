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

    // Cooldown tracking
    private long lastFireTime = 0;
    private long lastMoveTime = 0;

    // -------------------------------------------------------------------------
    //  EXPLOSION SYSTEM
    //  Each Explosion tracks one tile and how far through its animation it is.
    //  The Timer ticks every 80ms, advancing all active explosions each tick.
    // -------------------------------------------------------------------------
    private ArrayList<Explosion> activeExplosions = new ArrayList<>();
    private Timer explosionTimer;
    private static final int EXPLOSION_FRAMES      = 7;  // regular shot frames
    private static final int MINE_EXPLOSION_FRAMES = 10; // mine explosion is bigger and longer

    // Inner class — one instance per explosion event.
    // isMine = true plays a bigger, longer explosion for mine detonations.
    private class Explosion {
        int row, col;
        int frame;
        boolean isMine;

        Explosion(int row, int col, boolean isMine) {
            this.row   = row;
            this.col   = col;
            this.frame = 0;
            this.isMine = isMine;
        }

        boolean isFinished() {
            return frame >= (isMine ? MINE_EXPLOSION_FRAMES : EXPLOSION_FRAMES);
        }

        void draw(Graphics2D g2d) {
            int x  = OFFSET_X + (col * TILE_SIZE);
            int y  = OFFSET_Y + (row * TILE_SIZE);
            int cx = x + TILE_SIZE / 2;
            int cy = y + TILE_SIZE / 2;

            if (isMine) {
                // Mine explosion: bigger, longer, with outer shockwave ring
                int[] sizes = {8, 14, 20, 28, 34, 30, 26, 20, 14, 7};
                int size = sizes[frame];

                // Shockwave ring on frames 2-6
                if (frame >= 2 && frame <= 6) {
                    int ring = size + 8;
                    g2d.setColor(new Color(255, 200, 0, 120));
                    g2d.fillOval(cx - ring, cy - ring, ring * 2, ring * 2);
                }

                if (frame <= 3) {
                    // Bright orange fireball
                    g2d.setColor(new Color(255, 90, 0));
                    g2d.fillOval(cx - size, cy - size, size * 2, size * 2);
                    int inner = size / 2;
                    g2d.setColor(new Color(255, 240, 100));
                    g2d.fillOval(cx - inner, cy - inner, inner * 2, inner * 2);
                } else if (frame <= 6) {
                    // Peak — deep red with white center
                    g2d.setColor(new Color(200, 30, 0));
                    g2d.fillOval(cx - size, cy - size, size * 2, size * 2);
                    int inner = size / 2;
                    g2d.setColor(Color.WHITE);
                    g2d.fillOval(cx - inner, cy - inner, inner * 2, inner * 2);
                } else {
                    // Long smoke trail
                    g2d.setColor(new Color(60, 60, 60, 160));
                    g2d.fillOval(cx - size, cy - size, size * 2, size * 2);
                }

            } else {
                // Regular shot explosion
                int[] sizes = {6, 12, 18, 24, 22, 16, 8};
                int size = sizes[frame];

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

    // -------------------------------------------------------------------------
    //  CONSTRUCTOR
    // -------------------------------------------------------------------------
    public GameCanvas(Grid arena, DataOutputStream o, Ship myShip) {
        this.sharedArena    = arena;
        this.out            = o;
        this.controlledShip = myShip;

        setupKeyBindings();
        setupExplosionTimer();

        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!gameActive) return;

                long currentTime = System.currentTimeMillis();
                if (currentTime - lastFireTime < 1000) return;

                int col = (e.getX() - OFFSET_X) / TILE_SIZE;
                int row = (e.getY() - OFFSET_Y) / TILE_SIZE;

                if (row >= 0 && row < Grid.SIZE && col >= 0 && col < Grid.SIZE) {
                    try {
                        out.writeUTF("FIRE:" + row + ":" + col);
                        out.flush();
                        lastFireTime = currentTime;
                    } catch (IOException ex) {}
                }
            }
        });
    }

    // -------------------------------------------------------------------------
    //  EXPLOSION TIMER
    //  Ticks every 80ms. Advances all active explosions, removes finished ones.
    // -------------------------------------------------------------------------
    private void setupExplosionTimer() {
        explosionTimer = new Timer(80, new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                for (Explosion ex : activeExplosions) ex.frame++;
                activeExplosions.removeIf(ex -> ex.isFinished());
                repaint();
            }
        });
        explosionTimer.start();
    }

    /**
     * Called by NetworkThread when ATTACK_SYNC is received.
     * Starts a new explosion animation at the given tile.
     */
    /** Regular shot explosion — called by NetworkThread on ATTACK_SYNC. */
    public void addExplosion(int row, int col) {
        activeExplosions.add(new Explosion(row, col, false));
    }

    /** Mine explosion — bigger and longer, called by NetworkThread on MINE_SYNC. */
    public void addMineExplosion(int row, int col) {
        activeExplosions.add(new Explosion(row, col, true));
    }

    public void setGameActive(boolean active) { this.gameActive = active; }

    // -------------------------------------------------------------------------
    //  KEY BINDINGS — same as your original
    // -------------------------------------------------------------------------
    private void setupKeyBindings() {
        InputMap  im = this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = this.getActionMap();

        im.put(KeyStroke.getKeyStroke("UP"),    "U");
        im.put(KeyStroke.getKeyStroke("DOWN"),  "D");
        im.put(KeyStroke.getKeyStroke("LEFT"),  "L");
        im.put(KeyStroke.getKeyStroke("RIGHT"), "R");
        im.put(KeyStroke.getKeyStroke("SPACE"), "S");

        am.put("U", new AbstractAction() { public void actionPerformed(ActionEvent e) { requestMove(-1,  0, controlledShip.isHorizontal()); }});
        am.put("D", new AbstractAction() { public void actionPerformed(ActionEvent e) { requestMove( 1,  0, controlledShip.isHorizontal()); }});
        am.put("L", new AbstractAction() { public void actionPerformed(ActionEvent e) { requestMove( 0, -1, controlledShip.isHorizontal()); }});
        am.put("R", new AbstractAction() { public void actionPerformed(ActionEvent e) { requestMove( 0,  1, controlledShip.isHorizontal()); }});
        am.put("S", new AbstractAction() { public void actionPerformed(ActionEvent e) { requestMove( 0,  0, !controlledShip.isHorizontal()); }});
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
        } catch (IOException ex) {}
    }

    // -------------------------------------------------------------------------
    //  PAINT — grid first, then explosions on top
    // -------------------------------------------------------------------------
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        g2d.setColor(Color.BLACK);
        g2d.drawString("BATTLE ARENA", OFFSET_X, OFFSET_Y - 10);

        // LAYER 1: Tiles (unchanged from your original)
        for (int r = 0; r < Grid.SIZE; r++) {
            for (int c = 0; c < Grid.SIZE; c++) {
                int s = sharedArena.getTileStatus(r, c);

                if      (s == Grid.WATER)  g2d.setColor(new Color(0, 119, 190));
                else if (s == Grid.SHIP)   g2d.setColor(Color.DARK_GRAY);
                else if (s == Grid.MISS)   g2d.setColor(Color.WHITE);
                else if (s == Grid.HIT)    g2d.setColor(Color.RED);
                else if (s == Grid.ISLAND) g2d.setColor(new Color(34, 139, 34));
                else if (s == Grid.MINE)   g2d.setColor(Color.ORANGE);

                int x = OFFSET_X + (c * TILE_SIZE);
                int y = OFFSET_Y + (r * TILE_SIZE);
                g2d.fillRect(x, y, TILE_SIZE, TILE_SIZE);
                g2d.setColor(Color.BLACK);
                g2d.drawRect(x, y, TILE_SIZE, TILE_SIZE);
            }
        }

        // LAYER 2: Green outline on your ship (unchanged from your original)
        g2d.setColor(Color.GREEN);
        g2d.drawRect(
            OFFSET_X + (controlledShip.getStartCol() * TILE_SIZE),
            OFFSET_Y + (controlledShip.getStartRow() * TILE_SIZE),
            controlledShip.isHorizontal() ? TILE_SIZE * 3 : TILE_SIZE,
            controlledShip.isHorizontal() ? TILE_SIZE : TILE_SIZE * 3
        );

        // LAYER 3: Active explosions drawn on top of everything
        for (Explosion ex : activeExplosions) {
            ex.draw(g2d);
        }
    }
}
