import java.io.*;
import javax.swing.SwingUtilities;

/**
 * NetworkThread runs on its own thread so that blocking readUTF() calls
 * never freeze the Swing GUI thread.
 * 
 * It reads messages from the server and updates the game state accordingly,
 * then calls repaint() on the GameFrame's canvas to reflect changes.
 * 
 * All GUI updates are wrapped in SwingUtilities.invokeLater() per Swing rules.
 */
public class NetworkThread extends Thread {

    private DataInputStream in;
    private GameFrame       gameFrame;
    private Player          localPlayer;
    private Grid            enemyTrackingGrid;

    public NetworkThread(DataInputStream in, GameFrame gameFrame,
                         Player localPlayer, Grid enemyTrackingGrid) {
        this.in               = in;
        this.gameFrame        = gameFrame;
        this.localPlayer      = localPlayer;
        this.enemyTrackingGrid = enemyTrackingGrid;
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
                // Both players are connected; game begins
                SwingUtilities.invokeLater(() -> {
                    gameFrame.showMessage("Game started! Waiting for your turn...");
                });
                break;

            case "YOUR_TURN":
                // It is now this player's turn to fire
                SwingUtilities.invokeLater(() -> {
                    gameFrame.setMyTurn(true);
                    gameFrame.showMessage("Your turn! Click on the enemy grid to fire.");
                    gameFrame.repaintCanvas();
                });
                break;

            case "WAIT":
                // Opponent's turn; disable firing
                SwingUtilities.invokeLater(() -> {
                    gameFrame.setMyTurn(false);
                    gameFrame.showMessage("Waiting for opponent to fire...");
                    gameFrame.repaintCanvas();
                });
                break;

            case "RESULT":
                // "RESULT:<row>:<col>:<HIT|MISS>"
                // The shot WE fired — update the enemy tracking grid
                if (parts.length == 4) {
                    int row = Integer.parseInt(parts[1]);
                    int col = Integer.parseInt(parts[2]);
                    String outcome = parts[3];

                    SwingUtilities.invokeLater(() -> {
                        if (outcome.equals("HIT")) {
                            // Use setTileStatus directly — enemyTrackingGrid has no ships,
                            // so receiveAttack() would always write MISS instead of HIT.
                            enemyTrackingGrid.setTileStatus(row, col, Grid.HIT);
                            gameFrame.showMessage("HIT at [" + row + "][" + col + "]!");
                        } else {
                            enemyTrackingGrid.setTileStatus(row, col, Grid.MISS);
                            gameFrame.showMessage("Miss at [" + row + "][" + col + "].");
                        }
                        gameFrame.repaintCanvas();
                    });
                }
                break;

            case "OPPONENT_FIRED":
                // "OPPONENT_FIRED:<row>:<col>:<HIT|MISS>"
                // The shot the opponent fired at US — update our ocean grid display
                if (parts.length == 4) {
                    int row = Integer.parseInt(parts[1]);
                    int col = Integer.parseInt(parts[2]);
                    String outcome = parts[3];

                    SwingUtilities.invokeLater(() -> {
                        // The server already processed the hit on its side;
                        // we mirror it on our local ocean grid for display
                        localPlayer.getOceanGrid().receiveAttack(row, col);

                        if (outcome.equals("HIT")) {
                            gameFrame.showMessage("Opponent HIT your ship at [" + row + "][" + col + "]!");
                        } else {
                            gameFrame.showMessage("Opponent missed at [" + row + "][" + col + "].");
                        }
                        gameFrame.repaintCanvas();
                    });
                }
                break;

            case "RADAR_RESULT":
                // "RADAR_RESULT:<row>:<col>:<SHIP_NEARBY|CLEAR>"
                if (parts.length == 4) {
                    String radarOutcome = parts[3];
                    SwingUtilities.invokeLater(() -> {
                        if (radarOutcome.equals("SHIP_NEARBY")) {
                            gameFrame.showMessage("RADAR: Enemy vessel detected nearby!");
                        } else {
                            gameFrame.showMessage("RADAR: Sector is clear.");
                        }
                        gameFrame.repaintCanvas();
                    });
                }
                break;

            case "SUNK":
                // "SUNK:<shipName>"
                String sunkShipName = parts.length > 1 ? parts[1] : "a ship";
                SwingUtilities.invokeLater(() ->
                    gameFrame.showMessage("A ship was sunk: " + sunkShipName + "!")
                );
                break;

            case "GAMEOVER":
                // "GAMEOVER:<winnerNumber>"
                if (parts.length == 2) {
                    int winnerNumber = Integer.parseInt(parts[1]);
                    SwingUtilities.invokeLater(() -> {
                        gameFrame.setMyTurn(false);
                        int myNumber = gameFrame.getPlayerNumber();
                        if (winnerNumber == myNumber) {
                            gameFrame.showMessage("YOU WIN! All enemy ships sunk!");
                        } else {
                            gameFrame.showMessage("You lose. Opponent sunk all your ships.");
                        }
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
