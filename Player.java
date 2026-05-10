public class Player {
    private String name;
    private Grid oceanGrid; 
    
    private int normalAmmoCount;
    private int radarAmmoCount;
    private int areaAmmoCount;

    public Player(String name) {
        this.name = name;
        this.oceanGrid = new Grid();
        
        this.normalAmmoCount = 50; 
        this.radarAmmoCount = 3;   
        this.areaAmmoCount = 1;    
    }

    public Grid getOceanGrid() { return oceanGrid; }
    public String getName() { return name; }

    public boolean hasRadarAmmo() { return radarAmmoCount > 0; }
    public void useRadarAmmo() { if (hasRadarAmmo()) radarAmmoCount--; }
    
    public boolean hasAreaAmmo() { return areaAmmoCount > 0; }
    public void useAreaAmmo() { if (hasAreaAmmo()) areaAmmoCount--; }
}