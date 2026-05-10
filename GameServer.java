import java.io.*;
import java.net.*;
import java.util.ArrayList;

/**
 * GameServer manages the Battleship game server.
 * 
 * - Main thread: loops calling accept() to welcome exactly 2 players.
 * - Each player gets its own ConnectionThread (inner class) for I/O.
 * - The server enforces whose turn it is and broadcasts results.
 * 
 * Message protocol (plain strings sent via writeUTF / readUTF):
 *   Client → Server:
 *     "FIRE:<ammoType>:<row>:<col>"   e.g. "FIRE:NORMAL:3:5"
 *
 *   Server → Client:
 *     "ASSIGN:<playerNumber>"         e.g. "ASSIGN:1" or "ASSIGN:2"
 *     "START"                         both players connected, game begins
 *     "YOUR_TURN"                     it is now your turn to fire
 *     "WAIT"                          wait for your opponent's move
 *     "RESULT:<row>:<col>:<outcome>"  outcome = HIT or MISS
 *     "SUNK:<shipName>"               a ship was fully sunk
 *     "GAMEOVER:<winnerNumber>"       e.g. "GAMEOVER:1"
 *     "OPPONENT_FIRED:<row>:<col>:<outcome>"  apply this to YOUR ocean grid
 */
public class GameServer {

    public static final int PORT = 5000;
    public static final int MAX_PLAYERS = 2;

    // One Grid per player — the server tracks both fleets
    private Grid[] grids;

    // The two connection threads, stored so they can message each other
    private ArrayList<ConnectionThread> connections;

    // Whose turn it is: 0 = player 1, 1 = player 2
    private int currentTurn;

    // Whether the game is still running
    private boolean gameRunning;

    public GameServer() {
        grids = new Grid[MAX_PLAYERS];
        connections = new ArrayList<>();
        currentTurn = 0; // Player 1 always goes first
        gameRunning = false;

        // Set up each player's fleet on the server side
        setupFleet(0); // Player 1's fleet
        setupFleet(1); // Player 2's fleet
    }

    /**
     * Places the standard Battleship fleet on a grid.
     * Both players get the same ship sizes; positions are fixed for now.
     * (Later you can replace this with placement messages from clients.)
     */
    private void setupFleet(int playerIndex) {
        grids[playerIndex] = new Grid();

        // Carrier (5), Battleship (4), Cruiser (3), Submarine (3), Destroyer (2)
        Ship carrier    = new Ship("Carrier", 5);
        Ship battleship = new Ship("Battleship", 4);
        Ship cruiser    = new Ship("Cruiser", 3);
        Ship submarine  = new Ship("Submarine", 3);
        Ship destroyer  = new Ship("Destroyer", 2);

        if (playerIndex == 0) {
            grids[0].placeShip(carrier,    0, 0, true);
            grids[0].placeShip(battleship, 2, 1, true);
            grids[0].placeShip(cruiser,    4, 3, false);
            grids[0].placeShip(submarine,  6, 5, true);
            grids[0].placeShip(destroyer,  8, 7, false);
        } else {
            grids[1].placeShip(carrier,    1, 2, false);
            grids[1].placeShip(battleship, 0, 5, true);
            grids[1].placeShip(cruiser,    3, 0, true);
            grids[1].placeShip(submarine,  5, 7, false);
            grids[1].placeShip(destroyer,  8, 1, true);
        }
    }

    /**
     * Starts the server: opens a ServerSocket and loops accepting clients.
     * Each accepted Socket spawns a new ConnectionThread.
     */
    public void start() {
        System.out.println("GameServer starting on port " + PORT + "...");

        // Phase 1: Accept both players, then close the ServerSocket
        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Waiting for " + MAX_PLAYERS + " players to connect...");

            while (connections.size() < MAX_PLAYERS) {
                Socket clientSocket = serverSocket.accept(); // blocks until a client connects
                int playerNumber = connections.size() + 1;
                System.out.println("Player " + playerNumber + " connected: "
                        + clientSocket.getInetAddress().getHostAddress());

                ConnectionThread ct = new ConnectionThread(clientSocket, playerNumber);
                connections.add(ct);
                ct.start();
            }

            // No more connections needed — close the ServerSocket
            serverSocket.close();

        } catch (IOException e) {
            System.out.println("Server error during setup: " + e.getMessage());
            return;
        }

        // Phase 2: Start the game
        System.out.println("Both players connected. Starting game...");
        gameRunning = true;
        broadcast("START");
        connections.get(currentTurn).sendMessage("YOUR_TURN");
        connections.get(1 - currentTurn).sendMessage("WAIT");

        // Phase 3: Keep the main thread alive while ConnectionThreads do the work
        try {
            while (gameRunning) {
                Thread.sleep(500);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("Server shutting down.");
    }

    /**
     * Sends a message to ALL connected players.
     */
    private void broadcast(String message) {
        for (ConnectionThread ct : connections) {
            ct.sendMessage(message);
        }
    }

    /**
     * Called by a ConnectionThread when it receives a FIRE message.
     * 
     * @param firingPlayerIndex  0-based index of the player who fired
     * @param ammoType           "NORMAL", "AREA", or "RADAR"
     * @param row                target row
     * @param col                target column
     */
    private synchronized void handleFire(int firingPlayerIndex, String ammoType, int row, int col) {
        // Ignore if it is not this player's turn
        if (firingPlayerIndex != currentTurn) {
            connections.get(firingPlayerIndex).sendMessage("WAIT");
            return;
        }

        int targetPlayerIndex = 1 - firingPlayerIndex; // the opponent
        Grid targetGrid = grids[targetPlayerIndex];

        // RADAR is special — it does not mark tiles, it just scans.
        // Handle it first and return early before any weapon firing happens.
        if (ammoType.equals("RADAR")) {
            boolean found = false;
            for (int r = row - 1; r <= row + 1; r++) {
                for (int c = col - 1; c <= col + 1; c++) {
                    if (r >= 0 && r < Grid.SIZE && c >= 0 && c < Grid.SIZE) {
                        if (targetGrid.hasShipAt(r, c)) {
                            found = true;
                        }
                    }
                }
            }
            String radarResult = found ? "SHIP_NEARBY" : "CLEAR";
            connections.get(firingPlayerIndex).sendMessage("RADAR_RESULT:" + row + ":" + col + ":" + radarResult);

            // Switch turns after radar use
            currentTurn = targetPlayerIndex;
            connections.get(currentTurn).sendMessage("YOUR_TURN");
            connections.get(firingPlayerIndex).sendMessage("WAIT");
            return;
        }

        // Choose weapon — only AREA or NORMAL reach this point (RADAR returned early above)
        Ammo weapon;
        if (ammoType.equals("AREA")) {
            weapon = new AreaAmmo();
        } else {
            weapon = new NormalAmmo();
        }

        // Snapshot which ships were already sunk BEFORE this shot
        java.util.Set<String> alreadySunk = new java.util.HashSet<>();
        for (Ship ship : targetGrid.getShips()) {
            if (ship.isSunk()) alreadySunk.add(ship.getName());
        }

        // Fire the weapon (this updates the server-side grid)
        weapon.fire(targetGrid, row, col);

        // Send RESULT messages for every affected tile.
        // NORMAL = 1 tile. AREA = up to 9 tiles in the 3x3 area.
        if (ammoType.equals("AREA")) {
            for (int r = row - 1; r <= row + 1; r++) {
                for (int c = col - 1; c <= col + 1; c++) {
                    if (r >= 0 && r < Grid.SIZE && c >= 0 && c < Grid.SIZE) {
                        int tileOutcome = targetGrid.getTileStatus(r, c);
                        String tileStr = (tileOutcome == Grid.HIT) ? "HIT" : "MISS";
                        connections.get(firingPlayerIndex)
                                   .sendMessage("RESULT:" + r + ":" + c + ":" + tileStr);
                        connections.get(targetPlayerIndex)
                                   .sendMessage("OPPONENT_FIRED:" + r + ":" + c + ":" + tileStr);
                    }
                }
            }
        } else {
            // NORMAL ammo — single tile only
            int outcome = targetGrid.getTileStatus(row, col);
            String outcomeStr = (outcome == Grid.HIT) ? "HIT" : "MISS";
            connections.get(firingPlayerIndex)
                       .sendMessage("RESULT:" + row + ":" + col + ":" + outcomeStr);
            connections.get(targetPlayerIndex)
                       .sendMessage("OPPONENT_FIRED:" + row + ":" + col + ":" + outcomeStr);
        }

        // Broadcast only ships that are NEWLY sunk this turn
        for (Ship ship : targetGrid.getShips()) {
            if (ship.isSunk() && !alreadySunk.contains(ship.getName())) {
                broadcast("SUNK:" + ship.getName());
            }
        }

        // Check win condition
        if (targetGrid.areAllShipsSunk()) {
            int winnerNumber = firingPlayerIndex + 1; // back to 1-based
            broadcast("GAMEOVER:" + winnerNumber);
            gameRunning = false;
            return;
        }

        // Switch turns
        currentTurn = targetPlayerIndex;
        connections.get(currentTurn).sendMessage("YOUR_TURN");
        connections.get(firingPlayerIndex).sendMessage("WAIT");
    }

    // =========================================================================
    //  INNER CLASS: ConnectionThread
    //  One instance per connected client. Reads messages in a loop.
    // =========================================================================
    private class ConnectionThread extends Thread {

        private Socket socket;
        private int playerNumber;     // 1-based
        private int playerIndex;      // 0-based (playerNumber - 1)
        private DataInputStream  in;
        private DataOutputStream out;

        public ConnectionThread(Socket socket, int playerNumber) {
            this.socket = socket;
            this.playerNumber = playerNumber;
            this.playerIndex  = playerNumber - 1;
        }

        /** Safely sends a UTF string to this client. */
        public void sendMessage(String message) {
            try {
                out.writeUTF(message);
                out.flush();
            } catch (IOException e) {
                System.out.println("Failed to send to Player " + playerNumber + ": " + e.getMessage());
            }
        }

        @Override
        public void run() {
            try {
                in  = new DataInputStream(socket.getInputStream());
                out = new DataOutputStream(socket.getOutputStream());

                // Tell the client which player number they are
                sendMessage("ASSIGN:" + playerNumber);

                // Loop: keep reading messages from this client.
                // We rely on the IOException from readUTF() to break the loop
                // when the game ends or the client disconnects.
                while (true) {
                    String message = in.readUTF(); // blocks until client sends something
                    System.out.println("Received from Player " + playerNumber + ": " + message);
                    parseMessage(message);
                }

            } catch (IOException e) {
                System.out.println("Player " + playerNumber + " disconnected.");
                gameRunning = false;
            } finally {
                try { socket.close(); } catch (IOException e) { /* ignore */ }
            }
        }

        /** Parses incoming messages from the client and routes them. */
        private void parseMessage(String message) {
            String[] parts = message.split(":");

            switch (parts[0]) {
                case "FIRE":
                    // Format: "FIRE:<ammoType>:<row>:<col>"
                    if (parts.length == 4) {
                        String ammoType = parts[1];
                        int row = Integer.parseInt(parts[2]);
                        int col = Integer.parseInt(parts[3]);
                        handleFire(playerIndex, ammoType, row, col);
                    }
                    break;

                default:
                    System.out.println("Unknown message from Player " + playerNumber + ": " + message);
                    break;
            }
        }
    }

    // =========================================================================
    //  MAIN METHOD
    // =========================================================================
    public static void main(String[] args) {
        GameServer server = new GameServer();
        server.start();
    }
}
