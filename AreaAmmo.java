public class AreaAmmo extends Ammo {
    public AreaAmmo() {
        super("Area Clear Shell");
    }

    @Override
    public void fire(Grid targetGrid, int targetRow, int targetCol) {
        for (int r = targetRow - 1; r <= targetRow + 1; r++) {
            for (int c = targetCol - 1; c <= targetCol + 1; c++) {
                targetGrid.receiveAttack(r, c);
            }
        }
    }
}