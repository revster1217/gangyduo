/**
 * This abstract class serves as the foundational blueprint for all special moves in the game.
 * It provides a standardized execution structure that allows different abilities to interact with the game state.
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

public abstract class Ability {
    protected String name;

    /**
     * Constructs an Ability with a specific name.
     * @param name The descriptive name of the ability.
     */
    public Ability(String name) { this.name = name; }
    
    /**
     * Executes the specific logic associated with the ability.
     * @param grid The game board where the ability takes effect.
     * @param server The server instance used to broadcast updates.
     */
    public abstract void execute(Grid grid, GameServer server);
}