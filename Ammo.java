public abstract class Ammo {
    private String name;

    public Ammo(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public abstract void fire(Grid targetGrid, int targetRow, int targetCol);
}