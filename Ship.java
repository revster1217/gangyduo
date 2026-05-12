public class Ship {
    private String name;
    private int length;
    private boolean[] hitSegments; // Remembers which parts are damaged
    private int startRow = -1, startCol = -1;
    private boolean isHorizontal;

    public Ship(String name, int length) {
        this.name = name;
        this.length = length;
        this.hitSegments = new boolean[length];
    }

    public void takeHitAt(int index) {
        if (index >= 0 && index < length) hitSegments[index] = true;
    }

    public boolean isSegmentHit(int index) { return hitSegments[index]; }

    public boolean isSunk() {
        for (boolean hit : hitSegments) if (!hit) return false;
        return true;
    }

    public String getName() { return name; }
    public int getLength() { return length; }
    public int getStartRow() { return startRow; }
    public int getStartCol() { return startCol; }
    public boolean isHorizontal() { return isHorizontal; }

    public void setPlacement(int r, int c, boolean h) {
        this.startRow = r; this.startCol = c; this.isHorizontal = h;
    }
}