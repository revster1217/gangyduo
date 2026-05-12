/**
 * This class implements the Smoke Screen ability which hides a ship from view.
 * It extends the Ability class to synchronize the smoked status across all network clients.
 *
 * @author Louver Dale Pastrana (254509), Carlos Angelo Sajul (255090)
 * @version May 12, 2026
 */

/******************************************************************************
* I have not discussed the Java language code in my program
* with anyone other than my instructor or the teaching assistants
* assigned to this course.
*
* I have not used Java language code obtained from another student,
* or any other unauthorized source, either modified or unmodified.
*
* If any Java language code or documentation used in my program
* was obtained from another source, such as a textbook or website,
* that has been clearly noted with a proper citation in the comments
* of my program.
******************************************************************************/

public class SmokeAbility extends Ability {
    private String shipName;

    /**
     * Constructs a SmokeAbility for a specific ship.
     * @param shipName The name of the ship utilizing the smoke screen.
     */
    public SmokeAbility(String shipName) {
        super("Smoke Screen");
        this.shipName = shipName;
    }

    /**
     * Broadcosts a synchronization message to activate the smoke effect.
     * @param grid The shared game board.
     * @param server The server managing the broadcast.
     */
    @Override
    public void execute(Grid grid, GameServer server) {
        server.broadcast("SMOKE_SYNC:" + shipName + ":ON");
    }
}