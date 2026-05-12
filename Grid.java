import java.util.ArrayList;
import java.util.List;

public class Grid {

    public static final int SIZE = 15; // Increased from 10
    public static final int WATER = 0, SHIP = 1, MISS = 2, HIT = 3;
    public static final int ISLAND = 4, MINE = 5; // New terrain types

    private int[][] board = new int[SIZE][SIZE];
    private List<Player> ships = new ArrayList<>();

    public int getTileStatus(int r, int c) { return board[r][c]; }
    public void setTileStatus(int r, int c, int s) { if (r >= 0 && r < SIZE && c >= 0 && c < SIZE) board[r][c] = s; }
    public List<Player> getShips() { return ships; }

    public Player getShipByName(String name) {
        for (Player s : ships) if (s.getName().equals(name)) return s;
        return null;
    }

    public boolean hasShipAt(int row, int col) {
    // Check if the board at these coordinates is a SHIP (1) or a HIT (3)
    return board[row][col] == SHIP || board[row][col] == HIT;
    }

    public void removeShipTrace(Player ship) {
        for (int i = 0; i < ship.getLength(); i++) {
            int r = ship.isHorizontal() ? ship.getStartRow() : ship.getStartRow() + i;
            int c = ship.isHorizontal() ? ship.getStartCol() + i : ship.getStartCol();
            if (board[r][c] == SHIP || board[r][c] == HIT) board[r][c] = WATER;
        }
    }

    public void addShipTrace(Player ship) {
        for (int i = 0; i < ship.getLength(); i++) {
            int r = ship.isHorizontal() ? ship.getStartRow() : ship.getStartRow() + i;
            int c = ship.isHorizontal() ? ship.getStartCol() + i : ship.getStartCol();
            // Draw damaged part if hit, otherwise draw healthy part
            board[r][c] = ship.isSegmentHit(i) ? HIT : SHIP; 
        }
    }

    public boolean isValidPlacement(Player ship, int r, int c, boolean h) {
        for (int i = 0; i < ship.getLength(); i++) {
            int checkR = h ? r : r + i;
            int checkC = h ? c + i : c;
            
            // Check boundaries using SIZE
            if (checkR < 0 || checkR >= SIZE || checkC < 0 || checkC >= SIZE) return false;
            
            int val = board[checkR][checkC];
            // Block moving into other ships, hit debris, or Islands
            if (val == SHIP || val == HIT || val == ISLAND) return false;
        }
        return true;
    }

    public void placeShip(Player s, int r, int c, boolean h) {
        if (s.getStartRow() != -1) removeShipTrace(s);
        s.setPlacement(r, c, h);
        if (!ships.contains(s)) ships.add(s);
        addShipTrace(s);
    }

    public int receiveAttack(int r, int c) {
        if (r < 0 || r >= SIZE || c < 0 || c >= SIZE) return -1;
        if (board[r][c] == WATER) {
            board[r][c] = MISS;
            return MISS;
        } else if (board[r][c] == SHIP) {
            board[r][c] = HIT;
            for (Player s : ships) {
                for (int i = 0; i < s.getLength(); i++) {
                    int sr = s.isHorizontal() ? s.getStartRow() : s.getStartRow() + i;
                    int sc = s.isHorizontal() ? s.getStartCol() + i : s.getStartCol();
                    if (sr == r && sc == c) {
                        s.takeHitAt(i);
                        return HIT;
                    }
                }
            }
        }
        return board[r][c];
    }
}