import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

public class GameCanvas extends JComponent {
    private Grid grid; 
    private Ship controlledShip; // The ship we are driving
    
    private final int TILE_SIZE = 40; 
    private final int OFFSET_X = 50;  
    private final int OFFSET_Y = 50;  

    // CONSTRUCTOR
    public GameCanvas(Grid grid, Ship controlledShip) {
        this.grid = grid;
        this.controlledShip = controlledShip;
        
        setupKeyBindings();

        // --- MOUSE LISTENER (MUST BE INSIDE THE CONSTRUCTOR) ---
        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                // Calculate which row and column were clicked
                int clickX = e.getX() - OFFSET_X;
                int clickY = e.getY() - OFFSET_Y;

                // Ensure the click was actually inside the grid area
                if (clickX >= 0 && clickY >= 0) {
                    int col = clickX / TILE_SIZE;
                    int row = clickY / TILE_SIZE;

                    // Ensure the row and col are within the 0-9 bounds
                    if (row < Grid.SIZE && col < Grid.SIZE) {
                        
                        // Choose your Ammo and Fire! 
                        Ammo currentWeapon = new NormalAmmo(); 
                        
                        System.out.println("Firing " + currentWeapon.getName() + " at [" + row + "][" + col + "]");
                        currentWeapon.fire(grid, row, col);
                        
                        // Redraw the screen to show the Hit or Miss markers
                        repaint();
                    }
                }
            }
        });
    }

    private void setupKeyBindings() {
        InputMap im = this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = this.getActionMap();

        im.put(KeyStroke.getKeyStroke("UP"), "moveUp");
        im.put(KeyStroke.getKeyStroke("DOWN"), "moveDown");
        im.put(KeyStroke.getKeyStroke("LEFT"), "moveLeft");
        im.put(KeyStroke.getKeyStroke("RIGHT"), "moveRight");
        im.put(KeyStroke.getKeyStroke("SPACE"), "rotate");

        am.put("moveUp", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { moveShip(-1, 0); }
        });
        am.put("moveDown", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { moveShip(1, 0); }
        });
        am.put("moveLeft", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { moveShip(0, -1); }
        });
        am.put("moveRight", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { moveShip(0, 1); }
        });
        am.put("rotate", new AbstractAction() {
            public void actionPerformed(ActionEvent e) { rotateShip(); }
        });
    }

    private void moveShip(int dRow, int dCol) {
        int newRow = controlledShip.getStartRow() + dRow;
        int newCol = controlledShip.getStartCol() + dCol;

        grid.removeShipTrace(controlledShip); 

        if (grid.isValidPlacement(controlledShip, newRow, newCol, controlledShip.isHorizontal())) {
            controlledShip.setPlacement(newRow, newCol, controlledShip.isHorizontal());
        }

        grid.addShipTrace(controlledShip); 
        repaint(); 
    }

    private void rotateShip() {
        grid.removeShipTrace(controlledShip);

        boolean newOrientation = !controlledShip.isHorizontal();
        
        if (grid.isValidPlacement(controlledShip, controlledShip.getStartRow(), controlledShip.getStartCol(), newOrientation)) {
            controlledShip.setPlacement(controlledShip.getStartRow(), controlledShip.getStartCol(), newOrientation);
        }

        grid.addShipTrace(controlledShip);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // LAYER 1: Draw the base Ocean Water Grid
        for (int row = 0; row < Grid.SIZE; row++) {
            for (int col = 0; col < Grid.SIZE; col++) {
                int x = OFFSET_X + (col * TILE_SIZE);
                int y = OFFSET_Y + (row * TILE_SIZE);

                g2d.setColor(new Color(0, 119, 190)); 
                g2d.fillRect(x, y, TILE_SIZE, TILE_SIZE);
                
                g2d.setColor(Color.BLACK);
                g2d.drawRect(x, y, TILE_SIZE, TILE_SIZE);
            }
        }

        // LAYER 2: Draw the Ships as Simple Rectangles
        for (Ship ship : grid.getShips()) {
            if (ship.isPlaced()) {
                g2d.setColor(Color.DARK_GRAY);

                int startX = OFFSET_X + (ship.getStartCol() * TILE_SIZE);
                int startY = OFFSET_Y + (ship.getStartRow() * TILE_SIZE);
                
                int shipWidth = ship.isHorizontal() ? (ship.getLength() * TILE_SIZE) : TILE_SIZE;
                int shipHeight = ship.isHorizontal() ? TILE_SIZE : (ship.getLength() * TILE_SIZE);

                g2d.fillRect(startX, startY, shipWidth, shipHeight);
                g2d.setColor(Color.BLACK);
                g2d.drawRect(startX, startY, shipWidth, shipHeight);
            }
        }

        // LAYER 3: Draw Hits and Misses
        for (int row = 0; row < Grid.SIZE; row++) {
            for (int col = 0; col < Grid.SIZE; col++) {
                int status = grid.getTileStatus(row, col);
                int x = OFFSET_X + (col * TILE_SIZE);
                int y = OFFSET_Y + (row * TILE_SIZE);

                if (status == Grid.HIT) {
                    g2d.setColor(Color.RED);
                    g2d.fillRect(x, y, TILE_SIZE, TILE_SIZE);
                    g2d.setColor(Color.BLACK);
                    g2d.drawRect(x, y, TILE_SIZE, TILE_SIZE);
                } else if (status == Grid.MISS) {
                    g2d.setColor(Color.WHITE);
                    g2d.fillRect(x, y, TILE_SIZE, TILE_SIZE);
                    g2d.setColor(Color.BLACK);
                    g2d.drawRect(x, y, TILE_SIZE, TILE_SIZE);
                }
            }
        }
    }
}