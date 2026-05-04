/*-----------------------------------------------------------------------------
 |
 |  Class Name:  PersonaManager
 |
 |  Purpose:  Manages AI personas for the LLM platform (Functionality 4).
 |            Personas define a personality or behavioral style for the
 |            assistant, driven by a system-level prompt string. Each
 |            persona is owned by a user and can be attached to messages.
 |
 |  Packages:  java.sql
 |             java.util.Scanner
 |
 |  Methods:
 |      menu()          - Displays sub-menu and routes to methods
 |      createPersona() - Adds a new persona to the database
 |      deletePersona() - Removes an existing persona from the database
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
        int userId = Integer.parseInt(scanner.nextLine().trim()); // FK -> ApplicationUser

        System.out.print("Persona name: ");
        String name = scanner.nextLine().trim();                  // display name for this persona

        System.out.print("Description: ");
        String description = scanner.nextLine().trim();           // short summary of the persona's style

        System.out.print("System prompt: ");
        String systemPrompt = scanner.nextLine().trim();          // full instruction string for the AI

        int newId = createPersona(conn, userId, name, description, systemPrompt); // generated persona PK
        if (newId != -1) {
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
     |                 if it exists.
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
        int personaId = Integer.parseInt(scanner.nextLine().trim()); // PK of persona to delete

        boolean success = deletePersona(conn, personaId); // result of the deletion attempt
        if (success) {
            conn.commit();
        }
    }

    /*-------------------------------------------------------------------------
     | Method: createPersona
     |
     | Purpose: Creates a new AI persona that can be associated with messages
     |          and conversations. Personas define a personality or behavioral
     |          style for the assistant, driven by a system-level prompt.
     |
     | Pre-condition:  A valid, open database connection is provided. userId
     |                 must reference an existing User row. name must be
     |                 non-null and non-empty.
     |
     | Post-condition: A new Persona row is inserted into the database.
     |
     | Parameters:
     |      conn         (in) - open Oracle database connection
     |      userId       (in) - ID of the user who owns this persona
     |      name         (in) - display name of the persona (e.g. "Professor")
     |      description  (in) - short summary of the persona's style
     |      systemPrompt (in) - the full instruction string given to the AI
     |                          when this persona is active
     |
     | Returns:  the generated persona_id, or -1 on failure
     *-----------------------------------------------------------------------*/
    public static int createPersona(Connection conn, int userId,
                                    String name, String description,
                                    String systemPrompt) throws SQLException {
        if (name == null || name.trim().isEmpty()) {
            System.out.println("Persona name must be non-empty.");
            return -1;
        }

        String sql = "INSERT INTO Persona (user_id, name, description, " // parameterized INSERT
                   + "system_prompt, created_at) "
                   + "VALUES (?, ?, ?, ?, SYSDATE)";

        PreparedStatement pstmt = conn.prepareStatement(sql, new String[]{"persona_id"});
        pstmt.setInt(1, userId);
        pstmt.setString(2, name.trim());
        pstmt.setString(3, description);
        pstmt.setString(4, systemPrompt);
        pstmt.executeUpdate();

        ResultSet rs = pstmt.getGeneratedKeys(); // holds the auto-generated persona_id
        if (rs.next()) {
            int newId = rs.getInt(1); // the newly created persona's PK
            System.out.println("Persona created with ID: " + newId);
            pstmt.close();
            return newId;
        }

        pstmt.close();
        return -1;
    }

    /*-------------------------------------------------------------------------
     | Method: deletePersona
     |
     | Purpose: Removes a persona record from the database, provided it is
     |          not currently the active template for more than five ongoing
     |          conversations. If the count exceeds five, deletion is aborted
     |          and the user is informed. Because messages may reference this
     |          persona, the persona_id foreign key on Message is set to NULL
     |          on delete (as defined in the schema).
     |
     | Pre-condition:  A valid, open database connection is provided.
     |                 personaId must reference an existing Persona row.
     |                 The schema's ON DELETE SET NULL constraint must be in
     |                 place on Message.persona_id. Conversation table must
     |                 have a persona_id column and a status column where
     |                 'Open' indicates an ongoing conversation.
     |
     | Post-condition: If the active conversation count is 5 or fewer, the
     |                 Persona row is removed and any Message rows that
     |                 referenced it now have persona_id = NULL. Otherwise,
     |                 no changes are made to the database.
     |
     | Parameters:
     |      conn      (in) - open Oracle database connection
     |      personaId (in) - ID of the persona to delete
     |
     | Returns:  true if the persona was deleted successfully, false otherwise
     *-----------------------------------------------------------------------*/
    public static boolean deletePersona(Connection conn, int personaId) throws SQLException {

        // Per spec: a persona cannot be deleted if it is the active template for
        // more than five ongoing conversations. Query the count before attempting
        // the delete.
        // NOTE: assumes Conversation table has a persona_id column and a status
        //       column where 'Open' indicates an ongoing conversation.
        String countSql = "SELECT COUNT(*) FROM Conversation " // count active conversations using this persona
                        + "WHERE persona_id = ? AND status = 'Open'";

        PreparedStatement countStmt = conn.prepareStatement(countSql);
        countStmt.setInt(1, personaId);

        ResultSet rs = countStmt.executeQuery();           // result of the active conversation count
        rs.next();
        int activeCount = rs.getInt(1); // number of ongoing conversations using this persona
        countStmt.close();

        if (activeCount > 5) {
            System.out.println("Cannot delete persona " + personaId + ": currently active in "
                    + activeCount + " ongoing conversations (max 5 allowed).");
            return false;
        }

        String sql = "DELETE FROM Persona WHERE persona_id = ?"; // targeted delete by PK

        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, personaId);

        int rows = pstmt.executeUpdate(); // number of rows deleted
        pstmt.close();

        if (rows == 1) {
            System.out.println("Persona " + personaId + " deleted successfully.");
            return true;
        }

        System.out.println("No persona found with ID: " + personaId);
        return false;
    }
}
