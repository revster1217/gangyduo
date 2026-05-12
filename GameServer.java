import java.io.*;
import java.net.*;
import java.util.ArrayList;

public class GameServer {
    private Grid sharedArena = new Grid();
    private Player p1Ship = new Player("P1_Ship", 3);
    private Player p2Ship = new Player("P2_Ship", 3);
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
            
            // Sync generated terrain to clients immediately
            for(int r = 0; r < Grid.SIZE; r++) {
                for(int c = 0; c < Grid.SIZE; c++) {
                    int type = sharedArena.getTileStatus(r, c);
                    if (type == Grid.ISLAND || type == Grid.MINE) {
                        broadcast("TILE_SYNC:" + r + ":" + c + ":" + type);
                    }
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
    }

    public void broadcast(String m) {
        for (ConnectionThread ct : connections) ct.sendMessage(m);
    }

    private void checkGameOver() {
            if (p1Ship.isSunk()) {
                broadcast("GAMEOVER:2"); // Player 2 wins
                gameRunning = false;
            } else if (p2Ship.isSunk()) {
                broadcast("GAMEOVER:1"); // Player 1 wins
                gameRunning = false;
            }
        }

    private synchronized void handleFire(int r, int c) {
    if (!gameRunning) return;
    int outcome = sharedArena.receiveAttack(r, c);
    String res = (outcome == Grid.HIT) ? "HIT" : "MISS";
    broadcast("ATTACK_SYNC:" + r + ":" + c + ":" + res);

    // Simplified check
    checkGameOver();
    }

    private synchronized void handleMove(String shipName, int r, int c, boolean h) {
    if (!gameRunning) return;
    Player p = sharedArena.getShipByName(shipName);
    if (p != null) {
        sharedArena.removeShipTrace(p);
        if (sharedArena.isValidPlacement(p, r, c, h)) {
            ArrayList<int[]> explodedMines = new ArrayList<>();
            for (int i = 0; i < p.getLength(); i++) {
                int cr = h ? r : r + i;
                int cc = h ? c + i : c;
                if (sharedArena.getTileStatus(cr, cc) == Grid.MINE) explodedMines.add(new int[]{cr, cc});
            }

            for (int[] m : explodedMines) {
                sharedArena.setTileStatus(m[0], m[1], Grid.WATER);
                broadcast("TILE_SYNC:" + m[0] + ":" + m[1] + ":" + Grid.WATER);
            }

            sharedArena.placeShip(p, r, c, h);
            broadcast("MOVE_SYNC:" + shipName + ":" + r + ":" + c + ":" + (h ? "H" : "V"));
            
            for (int[] m : explodedMines) {
                sharedArena.receiveAttack(m[0], m[1]);
                broadcast("ATTACK_SYNC:" + m[0] + ":" + m[1] + ":MINE_HIT");
            }

            // ADD THIS CHECK HERE
            if (!explodedMines.isEmpty()) {
                checkGameOver();
            }
        } else {
            sharedArena.addShipTrace(p);
        }
    }
}

    private synchronized void handleSmoke(String shipName) {
        broadcast("SMOKE_SYNC:" + shipName + ":ON");
        new Thread(() -> {
            try { Thread.sleep(5000); } catch (InterruptedException e) {}
            broadcast("SMOKE_SYNC:" + shipName + ":OFF");
        }).start();
    }

    private synchronized void handleClusterBomb() {
        if (!gameRunning) return;
        for (int i = 0; i < 3; i++) {
            int r = (int) (Math.random() * Grid.SIZE);
            int c = (int) (Math.random() * Grid.SIZE);
            handleFire(r, c);
        }
    }

    private void generateMapFeatures() {
        // --- UPDATED: Place 15 Islands (Mix of 2x2 and 1x2 shapes) ---
        for(int i = 0; i < 15; i++) {
            int shapeType = (int)(Math.random() * 3); // 0 = 2x2, 1 = 1x2 Horiz, 2 = 2x1 Vert
            int r = (int)(Math.random() * (Grid.SIZE - 4)) + 2;
            int c = (int)(Math.random() * (Grid.SIZE - 4)) + 2;

            // Spawn zone protection (don't trap players)
            if ((r < 4 && c < 6) || (r > Grid.SIZE - 6 && c > Grid.SIZE - 7)) {
                i--; continue;
            }

            if (shapeType == 0) { // 2x2 Square
                sharedArena.setTileStatus(r, c, Grid.ISLAND);
                sharedArena.setTileStatus(r + 1, c, Grid.ISLAND);
                sharedArena.setTileStatus(r, c + 1, Grid.ISLAND);
                sharedArena.setTileStatus(r + 1, c + 1, Grid.ISLAND);
            } else if (shapeType == 1) { // 1x2 Horizontal
                sharedArena.setTileStatus(r, c, Grid.ISLAND);
                sharedArena.setTileStatus(r, c + 1, Grid.ISLAND);
            } else { // 2x1 Vertical
                sharedArena.setTileStatus(r, c, Grid.ISLAND);
                sharedArena.setTileStatus(r + 1, c, Grid.ISLAND);
            }
        }

        // --- UPDATED: Place 15 Sea Mines ---
        for(int i = 0; i < 15; i++) {
            int r = (int)(Math.random() * Grid.SIZE);
            int c = (int)(Math.random() * Grid.SIZE);
            
            // Check for water and spawn protection
            if (sharedArena.getTileStatus(r, c) != Grid.WATER || (r < 3 && c < 5) || (r > Grid.SIZE - 4 && c > Grid.SIZE - 5)) {
                i--; // Retry if placement is invalid
                continue;
            }
            sharedArena.setTileStatus(r, c, Grid.MINE);
        }
    }

    private class ConnectionThread extends Thread {
        private Socket s; private DataInputStream in; private DataOutputStream out; 
        public ConnectionThread(Socket s, int pNum) {
            this.s = s;
            try { 
                in = new DataInputStream(s.getInputStream()); 
                out = new DataOutputStream(s.getOutputStream()); 
                sendMessage("ASSIGN:" + pNum); 
            } catch (IOException e) {}
        }
        public void sendMessage(String m) { try { out.writeUTF(m); out.flush(); } catch (IOException e) {} }
        
        @Override
        public void run() {
            try {
                while (true) {
                    String message = in.readUTF();
                    String[] p = message.split(":");
                    if (p[0].equals("FIRE")) {
                        handleFire(Integer.parseInt(p[1]), Integer.parseInt(p[2]));
                    } else if (p[0].equals("REQUEST_MOVE")) {
                        handleMove(p[1], Integer.parseInt(p[2]), Integer.parseInt(p[3]), p[4].equals("H"));
                    } else if (p[0].equals("ABILITY")) {
                        Ability selectedAbility = null;

                        if (p[1].equals("SMOKE")) {
                            selectedAbility = new SmokeAbility(p[2]); // p[2] is the ship name
                        } else if (p[1].equals("CLUSTER_BOMB")) {
                            selectedAbility = new ClusterAbility();
                        }

                        // This single line replaces all the old individual method calls
                        if (selectedAbility != null) {
                            selectedAbility.execute(sharedArena, GameServer.this); 
                        }
                    }
                }
            } catch (IOException e) {}
        }

    }

    public static void main(String[] args) { new GameServer().start(); }
}