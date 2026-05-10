public class NormalAmmo extends Ammo {
    public NormalAmmo() {
        super("Normal Shell");
    }

    @Override
    public void fire(Grid targetGrid, int targetRow, int targetCol) {
        targetGrid.receiveAttack(targetRow, targetCol);
    }
}