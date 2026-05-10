import javax.swing.JFrame;

public class GameFrame extends JFrame {
    
    // We now pass the controlled ship into the frame
    public GameFrame(Player player, Ship pilotShip) {
        setTitle("Battleship - " + player.getName() + "'s Fleet");
        setSize(800, 600); 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Pass both the grid AND the ship to the canvas
        GameCanvas canvas = new GameCanvas(player.getOceanGrid(), pilotShip);
        add(canvas);
    }
}