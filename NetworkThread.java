import java.io.*;
import javax.swing.SwingUtilities;

/**
 * NetworkThread runs on its own thread so that blocking readUTF() calls
 * never freeze the Swing GUI thread.
 * * It reads messages from the server and updates the game state accordingly,
 * then calls repaint() on the GameFrame's canvas to reflect changes.
 * * All GUI updates are wrapped in SwingUtilities.invokeLater() per Swing rules.
 */
public class NetworkThread extends Thread {

    private DataInputStream in;
    private GameFrame       gameFrame;
    private Grid            sharedArena; // FIX: Replaced Player and enemyTrackingGrid with sharedArena

    // FIX: Constructor now matches what GameStarter passes
    public NetworkThread(DataInputStream in, GameFrame gameFrame, Grid sharedArena) {
        this.in               = in;
        this.gameFrame        = gameFrame;
        this.sharedArena      = sharedArena;
    }

    @Override
    public void run() {
        try {
            // Loop continuously reading messages from the server
            while (true) {
                String message = in.readUTF(); // blocks until server sends something
                System.out.println("[NetworkThread] Received: " + message);
                parseMessage(message);
            }
        } catch (IOException e) {
            System.out.println("Disconnected from server.");
            SwingUtilities.invokeLater(() ->
                gameFrame.showMessage("Disconnected from server.")
            );
        }
    }

    private void parseMessage(String message) {
        String[] parts = message.split(":");

        switch (parts[0]) {
            case "START":
                // Both players are connected; unlock controls
                SwingUtilities.invokeLater(() -> {
                    gameFrame.showMessage("Game Started! Move with Arrows/Space, Click to Fire!");
                    gameFrame.setGameActive(true); // This unlocks the GameCanvas
                });
                break;

            case "MOVE_SYNC":
                // Server tells us a ship moved: MOVE_SYNC:shipName:row:col:H/V
                if (parts.length == 5) {
                    String shipName = parts[1];
                    int r = Integer.parseInt(parts[2]);
                    int c = Integer.parseInt(parts[3]);
                    boolean h = parts[4].equals("H");
                    
                    SwingUtilities.invokeLater(() -> {
                        Ship s = sharedArena.getShipByName(shipName);
                        if (s != null) {
                            sharedArena.placeShip(s, r, c, h);
                            gameFrame.repaintCanvas();
                        }
                    });
                }
                break;

            case "ATTACK_SYNC":
                // Server tells us a shot was fired: ATTACK_SYNC:row:col:HIT/MISS
                if (parts.length == 4) {
                    int r = Integer.parseInt(parts[1]);
                    int c = Integer.parseInt(parts[2]);
                    
                    SwingUtilities.invokeLater(() -> {
                        sharedArena.receiveAttack(r, c);
                        gameFrame.repaintCanvas();
                    });
                }
                break;

            case "GAMEOVER":
                // "GAMEOVER:<winnerNumber>"
                if (parts.length == 2) {
                    int winnerNumber = Integer.parseInt(parts[1]);
                    SwingUtilities.invokeLater(() -> {
                        gameFrame.setGameActive(false); // Lock controls again
                        int myNumber = gameFrame.getPlayerNumber();
                        if (winnerNumber == myNumber) {
                            gameFrame.showMessage("YOU WIN! Enemy ship sunk!");
                        } else {
                            gameFrame.showMessage("You lose. Opponent sunk your ship.");
                        }
                        gameFrame.repaintCanvas();
                    });
                }
                break;

                case "TILE_SYNC":
                // Server tells us about a terrain feature: TILE_SYNC:row:col:type
                if (parts.length == 4) {
                    int tr = Integer.parseInt(parts[1]);
                    int tc = Integer.parseInt(parts[2]);
                    int type = Integer.parseInt(parts[3]);
                    SwingUtilities.invokeLater(() -> {
                        sharedArena.setTileStatus(tr, tc, type);
                        gameFrame.repaintCanvas();
                    });
                }
                break;

            default:
                System.out.println("[NetworkThread] Unknown message: " + message);
                break;
        }
    }
}