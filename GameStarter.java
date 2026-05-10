import java.io.*;
import java.net.*;
import javax.swing.SwingUtilities;

/**
 * GameStarter is the entry point for the CLIENT side.
 * 
 * It:
 *   1. Connects to the GameServer via a Socket.
 *   2. Waits to receive the ASSIGN message to know if we are Player 1 or 2.
 *   3. Builds the Player and fleet.
 *   4. Launches the GameFrame (GUI).
 *   5. Starts a NetworkThread to handle all incoming server messages
 *      WITHOUT blocking the GUI thread.
 * 
 * Usage:
 *   Run GameServer first.
 *   Then run this on each player's machine.
 *   Change SERVER_IP to the host machine's IP address if running on different machines.
 */
public class GameStarter {

    // --- Change this to the server machine's IP when running on separate computers ---
    private static final String SERVER_IP = "127.0.0.1";
    private static final int    PORT      = GameServer.PORT;

    public static void main(String[] args) {
        System.out.println("Connecting to server at " + SERVER_IP + ":" + PORT + "...");

        try {
            // 1. Connect to the server
            Socket socket = new Socket(SERVER_IP, PORT);
            System.out.println("Connected to server!");

            DataInputStream  in  = new DataInputStream(socket.getInputStream());
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());

            // 2. Wait for ASSIGN message to learn our player number
            String assignMsg = in.readUTF(); // blocks briefly — fine here, before GUI opens
            int playerNumber = 1;
            if (assignMsg.startsWith("ASSIGN:")) {
                playerNumber = Integer.parseInt(assignMsg.split(":")[1]);
            }
            System.out.println("Assigned as Player " + playerNumber);

            // 3. Build this player's data
            Player localPlayer = new Player("Player " + playerNumber);

            // Place ships for this player's own ocean grid
            // (These are the ships the OPPONENT will try to sink)
            Ship carrier    = new Ship("Carrier",    5);
            Ship battleship = new Ship("Battleship", 4);
            Ship cruiser    = new Ship("Cruiser",    3);
            Ship submarine  = new Ship("Submarine",  3);
            Ship destroyer  = new Ship("Destroyer",  2);

            if (playerNumber == 1) {
                localPlayer.getOceanGrid().placeShip(carrier,    0, 0, true);
                localPlayer.getOceanGrid().placeShip(battleship, 2, 1, true);
                localPlayer.getOceanGrid().placeShip(cruiser,    4, 3, false);
                localPlayer.getOceanGrid().placeShip(submarine,  6, 5, true);
                localPlayer.getOceanGrid().placeShip(destroyer,  8, 7, false);
            } else {
                localPlayer.getOceanGrid().placeShip(carrier,    1, 2, false);
                localPlayer.getOceanGrid().placeShip(battleship, 0, 5, true);
                localPlayer.getOceanGrid().placeShip(cruiser,    3, 0, true);
                localPlayer.getOceanGrid().placeShip(submarine,  5, 7, false);
                localPlayer.getOceanGrid().placeShip(destroyer,  8, 1, true);
            }

            // 4. Enemy tracking grid — this is what we draw when we fire at the opponent
            //    It starts empty; hits/misses are filled in as the server reports results.
            Grid enemyTrackingGrid = new Grid();

            // The first ship in the fleet is the one the player controls with arrow keys
            // (same idea as original GameStarter: one "hero" ship you can move)
            final Ship controlledShip = carrier;

            // Final copies required for use inside the lambda
            final DataInputStream  finalIn  = in;
            final DataOutputStream finalOut = out;

            // 5. Launch GUI on the Event Dispatch Thread (Swing rule)
            final int finalPlayerNumber = playerNumber;
            SwingUtilities.invokeLater(() -> {
                GameFrame frame = new GameFrame(localPlayer, enemyTrackingGrid, finalOut, finalPlayerNumber);

                // Tell the canvas which ship the player controls with arrow keys
                frame.getCanvas().setControlledShip(controlledShip);

                frame.setVisible(true);

                // 6. Start the network thread AFTER the GUI is ready
                NetworkThread networkThread = new NetworkThread(finalIn, frame, localPlayer, enemyTrackingGrid);
                networkThread.start();
            });

        } catch (IOException e) {
            System.out.println("Could not connect to server: " + e.getMessage());
        }
    }
}
