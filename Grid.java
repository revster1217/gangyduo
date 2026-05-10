import java.util.ArrayList;
import java.util.List;

public class Grid {
    public static final int SIZE = 10;

    public static final int WATER = 0;
    public static final int SHIP  = 1;
    public static final int MISS  = 2;
    public static final int HIT   = 3;

    private int[][]    board;
    private List<Ship> ships;

    public Grid() {
        board = new int[SIZE][SIZE];
        ships = new ArrayList<>();
        for (int row = 0; row < SIZE; row++)
            for (int col = 0; col < SIZE; col++)
                board[row][col] = WATER;
    }

    public int getTileStatus(int row, int col) { return board[row][col]; }

    /** Directly sets a tile to HIT or MISS — used by the client tracking grid
     *  where no ships exist but we still need to display results from the server. */
    public void setTileStatus(int row, int col, int status) {
        if (row >= 0 && row < SIZE && col >= 0 && col < SIZE) {
            board[row][col] = status;
        }
    }
    /** Returns true if a ship occupies this tile, whether or not it has been hit already. */
    public boolean hasShipAt(int row, int col)  { return board[row][col] == SHIP || board[row][col] == HIT; }
    public List<Ship> getShips()                { return ships; }

    public void removeShipTrace(Ship ship) {
        for (int i = 0; i < ship.getLength(); i++) {
            int r = ship.isHorizontal() ? ship.getStartRow()     : ship.getStartRow() + i;
            int c = ship.isHorizontal() ? ship.getStartCol() + i : ship.getStartCol();
            // Only clear SHIP tiles back to WATER.
            // Leave HIT tiles as HIT — they are permanent attack markers.
            if (board[r][c] == SHIP) board[r][c] = WATER;
        }
    }

    public void addShipTrace(Ship ship) {
        for (int i = 0; i < ship.getLength(); i++) {
            if (ship.isHorizontal()) board[ship.getStartRow()][ship.getStartCol() + i] = SHIP;
            else                     board[ship.getStartRow() + i][ship.getStartCol()] = SHIP;
        }
    }

    public boolean isValidPlacement(Ship ship, int startRow, int startCol, boolean isHorizontal) {
        if (isHorizontal) {
            if (startCol + ship.getLength() > SIZE) return false;
            for (int i = 0; i < ship.getLength(); i++)
                if (board[startRow][startCol + i] != WATER) return false;
        } else {
            if (startRow + ship.getLength() > SIZE) return false;
            for (int i = 0; i < ship.getLength(); i++)
                if (board[startRow + i][startCol] != WATER) return false;
        }
        return true;
    }

    public boolean placeShip(Ship ship, int startRow, int startCol, boolean isHorizontal) {
        if (!isValidPlacement(ship, startRow, startCol, isHorizontal)) return false;
        ship.setPlacement(startRow, startCol, isHorizontal);
        ships.add(ship);
        addShipTrace(ship);
        return true;
    }

    /**
     * BUG FIX: Now calls ship.takeHit() when a ship tile is struck,
     * so that ship.isSunk() and areAllShipsSunk() actually work.
     */
    public int receiveAttack(int row, int col) {
        if (row < 0 || row >= SIZE || col < 0 || col >= SIZE) return -1;

        if (board[row][col] == WATER) {
            board[row][col] = MISS;
            return MISS;
        } else if (board[row][col] == SHIP) {
            board[row][col] = HIT;

            // Find the ship occupying this tile and register the hit
            for (Ship ship : ships) {
                if (ship.isPlaced() && occupiesTile(ship, row, col)) {
                    ship.takeHit();
                    break;
                }
            }
            return HIT;
        }
        return board[row][col]; // already hit or missed — return current status
    }

    /** Returns true if the given ship occupies the tile at (row, col). */
    private boolean occupiesTile(Ship ship, int row, int col) {
        for (int i = 0; i < ship.getLength(); i++) {
            if (ship.isHorizontal()) {
                if (ship.getStartRow() == row && ship.getStartCol() + i == col) return true;
            } else {
                if (ship.getStartRow() + i == row && ship.getStartCol() == col) return true;
            }
        }
        return false;
    }

    public boolean areAllShipsSunk() {
        for (Ship ship : ships) {
            if (!ship.isSunk()) return false;
        }
        return true;
    }
}
