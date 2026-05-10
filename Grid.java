import java.util.ArrayList;
import java.util.List;

public class Grid {
    public static final int SIZE = 10; 
    
    public static final int WATER = 0;
    public static final int SHIP = 1;
    public static final int MISS = 2;
    public static final int HIT = 3;

    private int[][] board; 
    private List<Ship> ships;

    public Grid() {
        board = new int[SIZE][SIZE];
        ships = new ArrayList<>();
        
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                board[row][col] = WATER;
            }
        }
    }

    public int getTileStatus(int row, int col) { return board[row][col]; }
    public boolean hasShipAt(int row, int col) { return board[row][col] == SHIP; }
    
    // --> NEW METHOD to let the Canvas see all ships
    public List<Ship> getShips() { return ships; } 

    // --> NEW METHOD to erase the ship's old position when moving
    public void removeShipTrace(Ship ship) {
        for (int i = 0; i < ship.getLength(); i++) {
            if (ship.isHorizontal()) {
                board[ship.getStartRow()][ship.getStartCol() + i] = WATER;
            } else {
                board[ship.getStartRow() + i][ship.getStartCol()] = WATER;
            }
        }
    }

    // --> NEW METHOD to stamp the ship's new position after moving
    public void addShipTrace(Ship ship) {
        for (int i = 0; i < ship.getLength(); i++) {
            if (ship.isHorizontal()) {
                board[ship.getStartRow()][ship.getStartCol() + i] = SHIP;
            } else {
                board[ship.getStartRow() + i][ship.getStartCol()] = SHIP;
            }
        }
    }

    public boolean isValidPlacement(Ship ship, int startRow, int startCol, boolean isHorizontal) {
        if (isHorizontal) {
            if (startCol + ship.getLength() > SIZE) return false; 
            for (int i = 0; i < ship.getLength(); i++) {
                if (board[startRow][startCol + i] != WATER) return false; 
            }
        } else {
            if (startRow + ship.getLength() > SIZE) return false; 
            for (int i = 0; i < ship.getLength(); i++) {
                if (board[startRow + i][startCol] != WATER) return false; 
            }
        }
        return true;
    }

    public boolean placeShip(Ship ship, int startRow, int startCol, boolean isHorizontal) {
        if (!isValidPlacement(ship, startRow, startCol, isHorizontal)) {
            return false;
        }
        ship.setPlacement(startRow, startCol, isHorizontal);
        ships.add(ship);
        
        // Use our new trace method to lock it into the grid
        addShipTrace(ship); 
        return true;
    }

    public int receiveAttack(int row, int col) {
        if (row < 0 || row >= SIZE || col < 0 || col >= SIZE) return -1; 

        if (board[row][col] == WATER) {
            board[row][col] = MISS;
            return MISS;
        } else if (board[row][col] == SHIP) {
            board[row][col] = HIT;
            return HIT;
        }
        return board[row][col]; 
    }

    public boolean areAllShipsSunk() {
        for (Ship ship : ships) {
            if (!ship.isSunk()) return false;
        }
        return true;
    }
}