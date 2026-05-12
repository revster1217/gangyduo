import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class GameServer {
    private Grid sharedArena = new Grid();
    private Ship p1Ship = new Ship("P1_Ship", 3);
    private Ship p2Ship = new Ship("P2_Ship", 3);
    private ArrayList<ConnectionThread> connections = new ArrayList<>();
    private boolean gameRunning = false;

    public GameServer() {
        generateMapFeatures();
        sharedArena.placeShip(p1Ship, 0, 0, true);
        sharedArena.placeShip(p2Ship, Grid.SIZE - 1, Grid.SIZE - 3, true); 
    }


    public void start() {
        try (ServerSocket ss = new ServerSocket(5000)) {
            System.out.println("Real-Time Server started on port 5000...");
            while (connections.size() < 2) {
                Socket s = ss.accept();
                ConnectionThread ct = new ConnectionThread(s, connections.size() + 1);
                connections.add(ct);
                ct.start();
            }
            gameRunning = true;
            broadcast("START");
            
            // NEW: Send the generated islands and mines to the clients
            for(int r = 0; r < Grid.SIZE; r++) {
                for(int c = 0; c < Grid.SIZE; c++) {
                    int type = sharedArena.getTileStatus(r, c);
                    if (type == Grid.ISLAND || type == Grid.MINE) {
                        broadcast("TILE_SYNC:" + r + ":" + c + ":" + type);
                    }
                }
            }
            System.out.println("Both connected! Match has begun.");
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void broadcast(String m) { for (ConnectionThread c : connections) c.sendMessage(m); }

    // SYNCHRONIZED: Prevents Race Conditions when both players move/shoot at the same time

    private synchronized void handleMove(String shipName, int r, int c, boolean h) {
        if (!gameRunning) return;
        Ship s = sharedArena.getShipByName(shipName);
        if (s != null) {
            sharedArena.removeShipTrace(s); // Temporarily lift ship to check the path
            
            if (sharedArena.isValidPlacement(s, r, c, h)) {
                // 1. Identify any mines in the ship's new path
                ArrayList<int[]> explodedMines = new ArrayList<>();
                for (int i = 0; i < s.getLength(); i++) {
                    int checkR = h ? r : r + i;
                    int checkC = h ? c + i : c;
                    if (sharedArena.getTileStatus(checkR, checkC) == Grid.MINE) {
                        explodedMines.add(new int[]{checkR, checkC});
                    }
                }

                // 2. Clear the mines and update the clients BEFORE moving the ship
                for (int[] mine : explodedMines) {
                    sharedArena.setTileStatus(mine[0], mine[1], Grid.WATER);
                    // Tell clients the mine is gone
                    broadcast("TILE_SYNC:" + mine[0] + ":" + mine[1] + ":" + Grid.WATER);
                }

                // 3. Place the ship in its new position and sync movement
                sharedArena.placeShip(s, r, c, h);
                broadcast("MOVE_SYNC:" + shipName + ":" + r + ":" + c + ":" + (h ? "H" : "V"));
                
                // 4. Apply damage AFTER the ship is placed so it registers as a HIT
                for (int[] mine : explodedMines) {
                    sharedArena.receiveAttack(mine[0], mine[1]); // Damages the ship object
                    broadcast("ATTACK_SYNC:" + mine[0] + ":" + mine[1] + ":HIT"); // Paints it red
                }
                
                // 5. Check if the mine sunk the ship
                if (p1Ship.isSunk()) { broadcast("GAMEOVER:2"); gameRunning = false; }
                else if (p2Ship.isSunk()) { broadcast("GAMEOVER:1"); gameRunning = false; }

            } else {
                sharedArena.addShipTrace(s); // Movement blocked, put it back
            }
        }
    }
    private synchronized void handleFire(int r, int c) {
        if (!gameRunning) return;
        int outcome = sharedArena.receiveAttack(r, c);
        String res = (outcome == Grid.HIT) ? "HIT" : "MISS";
        broadcast("ATTACK_SYNC:" + r + ":" + c + ":" + res);

        // Check wins in real-time
        if (p1Ship.isSunk()) { broadcast("GAMEOVER:2"); gameRunning = false; }
        else if (p2Ship.isSunk()) { broadcast("GAMEOVER:1"); gameRunning = false; }
    }

    private void generateMapFeatures() {
        // Place 10 Islands
        for(int i = 0; i < 10; i++) {
            int r = (int)(Math.random() * Grid.SIZE);
            int c = (int)(Math.random() * Grid.SIZE);
            // Don't spawn on top of starting corners
            if ((r < 3 && c < 3) || (r > Grid.SIZE-4 && c > Grid.SIZE-4)) continue;
            sharedArena.setTileStatus(r, c, Grid.ISLAND);
        }
        // Place 6 Sea Mines
        for(int i = 0; i < 6; i++) {
            int r = (int)(Math.random() * Grid.SIZE);
            int c = (int)(Math.random() * Grid.SIZE);
            if ((r < 3 && c < 3) || (r > Grid.SIZE-4 && c > Grid.SIZE-4) || sharedArena.getTileStatus(r, c) == Grid.ISLAND) continue;
            sharedArena.setTileStatus(r, c, Grid.MINE);
        }
    }

    private synchronized void handleSmoke(String shipName) {
        if (!gameRunning) return;
        
        // Turn smoke ON for both clients
        broadcast("SMOKE_SYNC:" + shipName + ":ON");
        
        // Start a background timer to turn it OFF after 5 seconds
        new Thread(() -> {
            try { Thread.sleep(5000); } catch (InterruptedException e) {}
            broadcast("SMOKE_SYNC:" + shipName + ":OFF");
        }).start();
    }

    private class ConnectionThread extends Thread {
        private Socket s; private DataInputStream in; private DataOutputStream out; 
        
        public ConnectionThread(Socket s, int pNum) {
            this.s = s;
            try { 
                in = new DataInputStream(s.getInputStream()); out = new DataOutputStream(s.getOutputStream()); 
                sendMessage("ASSIGN:" + pNum); 
            } catch (IOException e) {}
        }
        public void sendMessage(String m) { try { out.writeUTF(m); out.flush(); } catch (IOException e) {} }
        
        @Override
        public void run() {
            
            try {
                while (true) {
                    String[] p = in.readUTF().split(":");
                   if (p[0].equals("FIRE")) handleFire(Integer.parseInt(p[1]), Integer.parseInt(p[2]));
                    else if (p[0].equals("REQUEST_MOVE")) handleMove(p[1], Integer.parseInt(p[2]), Integer.parseInt(p[3]), p[4].equals("H"));
                    else if (p[0].equals("ABILITY") && p[1].equals("SMOKE")) handleSmoke(p[2]); // NEW LINE
                }
            } catch (IOException e) {}
        }
    }

    public static void main(String[] args) { new GameServer().start(); }
}