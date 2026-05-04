/*-----------------------------------------------------------------------------
 |
 |  Class Name:  ConversationManager
 |
 |  Purpose:  Manages conversations and messages for the LLM platform
 |            (Functionalities 2 and 4). Conversations group messages
 |            together under a user and workspace. Messages belong to a
 |            conversation and may optionally be linked to a persona.
 |            User feedback on individual messages is also handled here.
 |
 |  Packages:  java.sql
 |             java.util.Scanner
 |
 |  Methods:
 |      menu()              - Displays sub-menu and routes to methods
 |      startConversation() - Creates a new conversation for a user
 |      addMessage()        - Inserts a message into a conversation
 |      updateFeedback()    - Records or updates feedback on a message
 |
 *---------------------------------------------------------------------------*/

import java.sql.*;
import java.util.Scanner;

public class ConversationManager {

    /*-------------------------------------------------------------------------
     | Method: menu
     |
     | Purpose: Displays the conversation and message sub-menu and routes
     |          the user's choice to the appropriate method.
     |
     | Pre-condition:  A valid, open database connection is provided.
     |
     | Post-condition: One conversation or message operation is performed
     |                 based on user input.
     |
     | Parameters:
     |      conn    (in) - open Oracle database connection
     |      scanner (in) - Scanner object for reading user input
     |
     | Returns: Nothing
     *-----------------------------------------------------------------------*/
    public static void menu(Connection conn, Scanner scanner) throws SQLException {
        System.out.println("\n--- Conversations & Messages ---");
        System.out.println("1. Start a new conversation");
        System.out.println("2. Add a message to a conversation");
        System.out.println("3. Update message feedback");
        System.out.print("Choice: ");

        String choice = scanner.nextLine().trim(); // user's sub-menu selection

        switch (choice) {
            case "1": startConversationPrompt(conn, scanner); break;
            case "2": addMessagePrompt(conn, scanner);        break;
            case "3": updateFeedbackPrompt(conn, scanner);    break;
            default:  System.out.println("Invalid choice.");
        }
    }

    /*-------------------------------------------------------------------------
     | Method: startConversationPrompt
     |
     | Purpose: Collects user input for a new conversation and delegates
     |          to startConversation(). Commits the transaction on success.
     |
     | Pre-condition:  A valid, open database connection is provided.
     |
     | Post-condition: A new Conversation row is committed to the database
     |                 if input is valid.
     |
     | Parameters:
     |      conn    (in) - open Oracle database connection
     |      scanner (in) - Scanner object for reading user input
     |
     | Returns: Nothing
     *-----------------------------------------------------------------------*/
    private static void startConversationPrompt(Connection conn,
                                                Scanner scanner) throws SQLException {
        System.out.print("User ID: ");
        int userId = Integer.parseInt(scanner.nextLine().trim());      // FK -> ApplicationUser

        System.out.print("Workspace ID: ");
        int workspaceId = Integer.parseInt(scanner.nextLine().trim()); // FK -> Workspace

        System.out.print("Conversation title: ");
        String title = scanner.nextLine().trim();                      // descriptive name for this thread

        int newId = startConversation(conn, userId, workspaceId, title); // generated conversation PK
        if (newId != -1) {
            conn.commit();
        }
    }

    /*-------------------------------------------------------------------------
     | Method: addMessagePrompt
     |
     | Purpose: Collects user input for a new message and delegates to
     |          addMessage(). Commits the transaction on success.
     |
     | Pre-condition:  A valid, open database connection is provided.
     |
     | Post-condition: A new Message row is committed to the database
     |                 if input is valid.
     |
     | Parameters:
     |      conn    (in) - open Oracle database connection
     |      scanner (in) - Scanner object for reading user input
     |
     | Returns: Nothing
     *-----------------------------------------------------------------------*/
    private static void addMessagePrompt(Connection conn,
                                         Scanner scanner) throws SQLException {
        System.out.print("User ID: ");
        int userId = Integer.parseInt(scanner.nextLine().trim());          // FK -> ApplicationUser, needed for rate limit check

        // Verify the user hasn't exceeded their tier's daily message limit
        // NOTE: assumes UserManager.checkRateLimit(conn, userId) returns true if within limit
        if (!UserManager.checkRateLimit(conn, userId)) {
            System.out.println("Message limit reached for this user's subscription tier.");
            return;
        }

        System.out.print("Conversation ID: ");
        int conversationId = Integer.parseInt(scanner.nextLine().trim()); // FK -> Conversation

        System.out.print("Persona ID (-1 for none): ");
        int personaId = Integer.parseInt(scanner.nextLine().trim());      // FK -> Persona, or -1

        System.out.print("Role (user / assistant): ");
        String role = scanner.nextLine().trim();                          // sender type

        System.out.print("Message content: ");
        String content = scanner.nextLine().trim();                       // body text of the message

        System.out.print("Bookmark this message? (y/n): ");
        boolean bookmarked = scanner.nextLine().trim().equalsIgnoreCase("y"); // bookmark flag

        int newId = addMessage(conn, conversationId, personaId, role, content, bookmarked); // generated message PK
        if (newId != -1) {
            conn.commit();
        }
    }

    /*-------------------------------------------------------------------------
     | Method: updateFeedbackPrompt
     |
     | Purpose: Collects user input for message feedback and delegates to
     |          updateFeedback(). Commits the transaction on success.
     |
     | Pre-condition:  A valid, open database connection is provided.
     |
     | Post-condition: The Feedback table is updated and committed.
     |
     | Parameters:
     |      conn    (in) - open Oracle database connection
     |      scanner (in) - Scanner object for reading user input
     |
     | Returns: Nothing
     *-----------------------------------------------------------------------*/
    private static void updateFeedbackPrompt(Connection conn,
                                              Scanner scanner) throws SQLException {
        System.out.print("Message ID: ");
        int messageId = Integer.parseInt(scanner.nextLine().trim()); // FK -> Message

        System.out.print("Rating (1-5): ");
        int rating = Integer.parseInt(scanner.nextLine().trim());    // numeric user rating

        System.out.print("Comment (press Enter to skip): ");
        String comment = scanner.nextLine().trim();                  // optional text feedback
        if (comment.isEmpty()) {
            comment = null; // treat empty input as no comment
        }

        boolean success = updateFeedback(conn, messageId, rating, comment); // result of feedback save
        if (success) {
            conn.commit();
        }
    }

    /*-------------------------------------------------------------------------
     | Method: startConversation
     |
     | Purpose: Creates a new conversation record in the database for a given
     |          user and workspace. Inserts a row into the Conversation table
     |          and returns the generated conversation ID.
     |
     | Pre-condition:  A valid, open database connection is provided. userId
     |                 and workspaceId must reference existing rows in their
     |                 respective tables.
     |
     | Post-condition: A new Conversation row is inserted into the database.
     |
     | Parameters:
     |      conn        (in) - open Oracle database connection
     |      userId      (in) - ID of the user starting the conversation
     |      workspaceId (in) - ID of the workspace the conversation belongs to
     |      title       (in) - human-readable title for the conversation
     |
     | Returns:  the generated conversation_id, or -1 on failure
     *-----------------------------------------------------------------------*/
    public static int startConversation(Connection conn, int userId,
                                        int workspaceId, String title) throws SQLException {
        String sql = "INSERT INTO Conversation (user_id, workspace_id, title, " // parameterized INSERT
                   + "created_at) VALUES (?, ?, ?, SYSDATE)";

        PreparedStatement pstmt = conn.prepareStatement(sql, new String[]{"conversation_id"});
        pstmt.setInt(1, userId);
        pstmt.setInt(2, workspaceId);
        pstmt.setString(3, title);
        pstmt.executeUpdate();

        ResultSet rs = pstmt.getGeneratedKeys(); // holds the auto-generated conversation_id
        if (rs.next()) {
            int newId = rs.getInt(1); // the newly created conversation's PK
            System.out.println("Conversation started with ID: " + newId);
            pstmt.close();
            return newId;
        }

        pstmt.close();
        return -1;
    }

    /*-------------------------------------------------------------------------
     | Method: addMessage
     |
     | Purpose: Inserts a new message into an existing conversation. The role
     |          field distinguishes between 'user' and 'assistant' messages.
     |          Optionally marks the message as bookmarked on creation.
     |
     | Pre-condition:  A valid, open database connection is provided.
     |                 conversationId must reference an existing Conversation
     |                 row. personaId may be NULL for user-side messages;
     |                 pass -1 to indicate no persona.
     |
     | Post-condition: A new Message row is inserted into the database.
     |
     | Parameters:
     |      conn           (in) - open Oracle database connection
     |      conversationId (in) - ID of the conversation this message belongs to
     |      personaId      (in) - ID of the persona (pass -1 for none)
     |      role           (in) - "user" or "assistant"
     |      content        (in) - text body of the message
     |      bookmarked     (in) - whether the message should be bookmarked
     |
     | Returns:  the generated message_id, or -1 on failure
     *-----------------------------------------------------------------------*/
    public static int addMessage(Connection conn, int conversationId,
                                 int personaId, String role,
                                 String content, boolean bookmarked) throws SQLException {
        String sql = "INSERT INTO Message (conversation_id, persona_id, role, " // parameterized INSERT
                   + "content, bookmarked, sent_at) "
                   + "VALUES (?, ?, ?, ?, ?, SYSDATE)";

        PreparedStatement pstmt = conn.prepareStatement(sql, new String[]{"message_id"});
        pstmt.setInt(1, conversationId);

        if (personaId < 0) {
            pstmt.setNull(2, Types.INTEGER); // no persona for this message
        } else {
            pstmt.setInt(2, personaId);      // FK -> Persona
        }

        pstmt.setString(3, role);
        pstmt.setString(4, content);

        if (bookmarked) pstmt.setInt(5, 1); // store bookmarked as 1
        else            pstmt.setInt(5, 0); // store not bookmarked as 0

        pstmt.executeUpdate();

        ResultSet rs = pstmt.getGeneratedKeys(); // holds the auto-generated message_id
        if (rs.next()) {
            int newId = rs.getInt(1); // the newly created message's PK
            System.out.println("Message added with ID: " + newId);
            pstmt.close();
            return newId;
        }

        pstmt.close();
        return -1;
    }

    /*-------------------------------------------------------------------------
     | Method: updateFeedback
     |
     | Purpose: Records or updates user feedback on a specific message.
     |          Uses Oracle's MERGE statement to insert a new feedback row
     |          if one does not yet exist, or update the existing row if it
     |          does, avoiding duplicate feedback entries per message.
     |
     | Pre-condition:  A valid, open database connection is provided.
     |                 messageId must reference an existing Message row.
     |                 rating should be within the agreed range (e.g. 1-5).
     |
     | Post-condition: The Feedback table reflects the latest rating and
     |                 comment for the given message.
     |
     | Parameters:
     |      conn      (in) - open Oracle database connection
     |      messageId (in) - ID of the message being rated
     |      rating    (in) - numeric rating provided by the user
     |      comment   (in) - optional text feedback (may be null)
     |
     | Returns:  true if feedback was saved successfully, false otherwise
     *-----------------------------------------------------------------------*/
    public static boolean updateFeedback(Connection conn, int messageId,
                                         int rating, String comment) throws SQLException {
        String sql = "MERGE INTO Feedback f "                               // insert or update feedback row
                   + "USING (SELECT ? AS message_id FROM dual) src "
                   + "ON (f.message_id = src.message_id) "
                   + "WHEN MATCHED THEN "
                   + "  UPDATE SET f.rating = ?, f.comment = ?, "
                   + "             f.updated_at = SYSDATE "
                   + "WHEN NOT MATCHED THEN "
                   + "  INSERT (message_id, rating, comment, updated_at) "
                   + "  VALUES (?, ?, ?, SYSDATE)";

        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, messageId);    // USING clause source value
        pstmt.setInt(2, rating);       // UPDATE: new rating
        pstmt.setString(3, comment);   // UPDATE: new comment
        pstmt.setInt(4, messageId);    // INSERT: message FK
        pstmt.setInt(5, rating);       // INSERT: initial rating
        pstmt.setString(6, comment);   // INSERT: initial comment

        int rows = pstmt.executeUpdate(); // number of rows affected by MERGE
        pstmt.close();

        if (rows > 0) {
            System.out.println("Feedback updated for message ID: " + messageId);
            return true;
        }

        return false;
    }
}
