/**
 * Player holds the local player's data:
 *   - their name
 *   - their ocean grid (where their ships live)
 *   - their special ammo counts
 */
public class Player {
    private String name;
    private Grid   oceanGrid;

    private int normalAmmoCount;
    private int radarAmmoCount;
    private int areaAmmoCount;

    public Player(String name) {
        this.name  = name;
        this.oceanGrid = new Grid();

        this.normalAmmoCount = 50;
        this.radarAmmoCount  = 3;
        this.areaAmmoCount   = 1;
    }

    // --- Getters ---
    public Grid   getOceanGrid()      { return oceanGrid; }
    public String getName()           { return name; }
    public int    getRadarAmmoCount() { return radarAmmoCount; }
    public int    getAreaAmmoCount()  { return areaAmmoCount; }

    // --- Ammo usage ---
    public boolean hasRadarAmmo() { return radarAmmoCount > 0; }
    public void    useRadarAmmo() { if (hasRadarAmmo()) radarAmmoCount--; }

    public boolean hasAreaAmmo()  { return areaAmmoCount > 0; }
    public void    useAreaAmmo()  { if (hasAreaAmmo())  areaAmmoCount--; }
}
