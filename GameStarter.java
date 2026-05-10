public class GameStarter {
    public static void main(String[] args) {
        Player p1 = new Player("Player 1");
        
        // 1. Create your hero ship that you will control
        Ship myCruiser = new Ship("Hero Cruiser", 3);
        p1.getOceanGrid().placeShip(myCruiser, 4, 4, true);

        // 2. NEW: Create a stationary "Enemy" target ship 
        Ship enemyBattleship = new Ship("Enemy Target", 4);
        p1.getOceanGrid().placeShip(enemyBattleship, 1, 1, false); // Vertical ship at the top left

        // 3. Launch the GUI
        GameFrame frame = new GameFrame(p1, myCruiser);
        frame.setVisible(true);
    }
}