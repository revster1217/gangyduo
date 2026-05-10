public class RadarAmmo extends Ammo {
    public RadarAmmo() {
        super("Radar Ping");
    }

    @Override
    public void fire(Grid targetGrid, int targetRow, int targetCol) {
        boolean foundShip = false;
        
        for (int r = targetRow - 1; r <= targetRow + 1; r++) {
            for (int c = targetCol - 1; c <= targetCol + 1; c++) {
                 if (r >= 0 && r < Grid.SIZE && c >= 0 && c < Grid.SIZE) {
                     if (targetGrid.hasShipAt(r, c)) {
                         foundShip = true;
                     }
                 }
            }
        }
        
        if (foundShip) {
            System.out.println("RADAR ALERT: Enemy vessel detected in sector!");
        } else {
            System.out.println("Radar clear. Sector is empty.");
        }
    }
}