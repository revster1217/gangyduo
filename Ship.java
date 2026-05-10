public class Ship {
    private String name;
    private int length;
    private int health;
    private boolean isHorizontal;
    private boolean isPlaced;
    
    private int startRow;
    private int startCol;

    public Ship(String name, int length) {
        this.name = name;
        this.length = length;
        this.health = length;
        this.isPlaced = false;
    }

    public boolean takeHit() {
        if (health > 0) {
            health--;
        }
        return isSunk();
    }

    public boolean isSunk() { return health == 0; }

    public void setPlacement(int startRow, int startCol, boolean isHorizontal) {
        this.startRow = startRow;
        this.startCol = startCol;
        this.isHorizontal = isHorizontal;
        this.isPlaced = true;
    }

    public String getName() { return name; }
    public int getLength() { return length; }
    public boolean isHorizontal() { return isHorizontal; }
    public int getStartRow() { return startRow; }
    public int getStartCol() { return startCol; }
    public boolean isPlaced() { return isPlaced; }
}