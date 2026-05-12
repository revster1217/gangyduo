import java.io.*;
import java.net.*;
import javax.swing.SwingUtilities;

public class GameStarter {
    public static void main(String[] args) {
        try {
            Socket s = new Socket("127.0.0.1", 5000);
            DataInputStream in = new DataInputStream(s.getInputStream());
            DataOutputStream out = new DataOutputStream(s.getOutputStream());

            int pNum = Integer.parseInt(in.readUTF().split(":")[1]);
            
            // Client mirrors the Server's arena
            Grid sharedArena = new Grid();
            Ship p1 = new Ship("P1_Ship", 3);
            Ship p2 = new Ship("P2_Ship", 3);
            
            // --- THE FIX: Spawn the ships exactly where the GameServer spawns them ---
            sharedArena.placeShip(p1, 0, 0, true);
            sharedArena.placeShip(p2, Grid.SIZE - 1, Grid.SIZE - 3, true); 

            Ship myShip = (pNum == 1) ? p1 : p2;

            SwingUtilities.invokeLater(() -> {
                GameFrame f = new GameFrame(sharedArena, out, pNum, myShip);
                f.setVisible(true);
                new NetworkThread(in, f, sharedArena).start();
            });
        } catch (IOException e) { 
            e.printStackTrace(); 
        }
    }
}