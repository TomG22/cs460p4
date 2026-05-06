/*-----------------------------------------------------------------------------
 |
 |  Class Name:  PersonaManager
 |
 |  Purpose:  Manages AI personas for the LLM platform (Functionality 5).
 |            Personas define behavioral guidelines for the assistant via
 |            an instructions string. Each persona is owned by a user and
 |            can be attached to conversations. A persona cannot be deleted
 |            if it is actively linked to more than five ongoing conversations.
 |
 |  Packages:  java.sql
 |             java.util.Scanner
 |
 |  Methods:
 |      menu()                - Displays sub-menu and routes to methods
 |      createPersonaPrompt() - Collects input and calls createPersona()
 |      deletePersonaPrompt() - Collects input and calls deletePersona()
 |      createPersona()       - Inserts a new Persona row
 |      deletePersona()       - Removes a Persona row if guard conditions pass
 |
 *---------------------------------------------------------------------------*/

import java.sql.*;
import java.util.Scanner;

public class PersonaManager {

    /*-------------------------------------------------------------------------
     | Method: menu
     |
     | Purpose: Displays the persona management sub-menu and routes the
     |          user's choice to the appropriate method.
     |
     | Pre-condition:  A valid, open database connection is provided.
     |
     | Post-condition: One persona operation is performed based on user input.
     |
     | Parameters:
     |      conn    (in) - open Oracle database connection
     |      scanner (in) - Scanner object for reading user input
     |
     | Returns: Nothing
     *-----------------------------------------------------------------------*/
    public static void menu(Connection conn, Scanner scanner) throws SQLException {
        System.out.println("\n--- Persona Management ---");
        System.out.println("1. Create a persona");
        System.out.println("2. Delete a persona");
        System.out.print("Choice: ");

        String choice = scanner.nextLine().trim(); // user's sub-menu selection

        switch (choice) {
            case "1": createPersonaPrompt(conn, scanner); break;
            case "2": deletePersonaPrompt(conn, scanner); break;
            default:  System.out.println("Invalid choice.");
        }
    }

    /*-------------------------------------------------------------------------
     | Method: createPersonaPrompt
     |
     | Purpose: Collects user input for a new persona and delegates to
     |          createPersona(). Commits the transaction on success.
     |
     | Pre-condition:  A valid, open database connection is provided.
     |
     | Post-condition: A new Persona row is committed to the database
     |                 if input is valid.
     |
     | Parameters:
     |      conn    (in) - open Oracle database connection
     |      scanner (in) - Scanner object for reading user input
     |
     | Returns: Nothing
     *-----------------------------------------------------------------------*/
    private static void createPersonaPrompt(Connection conn,
                                             Scanner scanner) throws SQLException {
        System.out.print("User ID: ");
        int creatorID = Integer.parseInt(scanner.nextLine().trim()); // FK -> ApplicationUser

        System.out.print("Persona name: ");
        String name = scanner.nextLine().trim();                     // display name for this persona

        System.out.print("Instructions: ");
        String instructions = scanner.nextLine().trim();             // behavioral guidelines for the AI

        int newID = createPersona(conn, creatorID, name, instructions); // generated personalID
        if (newID != -1) {
            conn.commit();
        }
    }

    /*-------------------------------------------------------------------------
     | Method: deletePersonaPrompt
     |
     | Purpose: Collects user input for a persona deletion and delegates to
     |          deletePersona(). Commits the transaction on success.
     |
     | Pre-condition:  A valid, open database connection is provided.
     |
     | Post-condition: The specified Persona row is removed and committed
     |                 if the active conversation guard passes.
     |
     | Parameters:
     |      conn    (in) - open Oracle database connection
     |      scanner (in) - Scanner object for reading user input
     |
     | Returns: Nothing
     *-----------------------------------------------------------------------*/
    private static void deletePersonaPrompt(Connection conn,
                                             Scanner scanner) throws SQLException {
        System.out.print("Persona ID: ");
        int personalID = Integer.parseInt(scanner.nextLine().trim()); // PK of persona to delete

        boolean success = deletePersona(conn, personalID); // result of the deletion attempt
        if (success) {
            conn.commit();
        }
    }

    /*-------------------------------------------------------------------------
     | Method: createPersona
     |
     | Purpose: Creates a new AI persona that can be attached to conversations.
     |          Personas define behavioral guidelines for the assistant via
     |          an instructions string.
     |
     | Pre-condition:  A valid, open database connection is provided. creatorID
     |                 must reference an existing ApplicationUser row. name
     |                 must be non-null and non-empty.
     |
     | Post-condition: A new Persona row is inserted into the database.
     |
     | Parameters:
     |      conn         (in) - open Oracle database connection
     |      creatorID    (in) - ID of the user who owns this persona
     |      name         (in) - display name of the persona (e.g. "Professor")
     |      instructions (in) - behavioral guidelines string given to the AI
     |
     | Returns:  the generated personalID, or -1 on failure
     *-----------------------------------------------------------------------*/
    public static int createPersona(Connection conn, int creatorID,
                                    String name, String instructions) throws SQLException {
        if (name == null || name.trim().isEmpty()) {
            System.out.println("Persona name must be non-empty.");
            return -1;
        }

        String sql = "INSERT INTO asbarnica.Persona (personalID, creatorID, \"name\", " // parameterized INSERT
                   + "instructions, creationDate) "
                   + "VALUES (SEQ_PERSONA.NEXTVAL, ?, ?, ?, SYSDATE)";

        PreparedStatement pstmt = conn.prepareStatement(sql, new String[]{"personalID"});
        pstmt.setInt(1, creatorID);
        pstmt.setString(2, name.trim());
        pstmt.setString(3, instructions);
        pstmt.executeUpdate();

        ResultSet rs = pstmt.getGeneratedKeys(); // holds the auto-generated personalID
        if (rs.next()) {
            int newID = rs.getInt(1); // the newly created persona's PK
            System.out.println("Persona created with ID: " + newID);
            pstmt.close();
            return newID;
        }

        pstmt.close();
        return -1;
    }

    /*-------------------------------------------------------------------------
     | Method: deletePersona
     |
     | Purpose: Removes a persona record from the database, provided it is
     |          not currently attached to more than five active conversations.
     |          If the count exceeds five, deletion is aborted and the user
     |          is informed. Because Conversation references Persona with
     |          ON DELETE SET NULL, any remaining references are nulled out.
     |
     | Pre-condition:  A valid, open database connection is provided.
     |                 personalID must reference an existing Persona row.
     |                 Conversation.activeStatus = 1 indicates an ongoing
     |                 conversation.
     |
     | Post-condition: If the active conversation count is 5 or fewer, the
     |                 Persona row is removed and any Conversation rows that
     |                 referenced it now have personaID = NULL. Otherwise,
     |                 no changes are made to the database.
     |
     | Parameters:
     |      conn       (in) - open Oracle database connection
     |      personalID (in) - ID of the persona to delete
     |
     | Returns:  true if the persona was deleted successfully, false otherwise
     *-----------------------------------------------------------------------*/
    public static boolean deletePersona(Connection conn, int personalID) throws SQLException {

        // Per spec: abort deletion if persona is active in more than 5 ongoing conversations
        String countSql = "SELECT COUNT(*) FROM asbarnica.Conversation "  // count active conversations using this persona
                        + "WHERE personaID = ? AND activeStatus = 1";

        PreparedStatement countStmt = conn.prepareStatement(countSql);
        countStmt.setInt(1, personalID);

        ResultSet rs = countStmt.executeQuery();
        rs.next();
        int activeCount = rs.getInt(1); // number of ongoing conversations using this persona
        countStmt.close();

        if (activeCount > 5) {
            System.out.println("Cannot delete persona " + personalID + ": currently active in "
                    + activeCount + " ongoing conversations (max 5 allowed).");
            return false;
        }

        String sql = "DELETE FROM asbarnica.Persona WHERE personalID = ?"; // targeted delete by PK

        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, personalID);

        int rows = pstmt.executeUpdate(); // number of rows deleted
        pstmt.close();

        if (rows == 1) {
            System.out.println("Persona " + personalID + " deleted successfully.");
            return true;
        }

        System.out.println("No persona found with ID: " + personalID);
        return false;
    }
}
