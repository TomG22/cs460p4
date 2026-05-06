/*-----------------------------------------------------------------------------
 |
 |  Class Name:  ConversationManager
 |
 |  Purpose:  Manages conversations and messages for the LLM platform
 |            (Functionalities 2 and 4). Conversations group messages
 |            together under a user and workspace. Messages belong to a
 |            conversation and may optionally be linked to a persona.
 |            User feedback on individual messages is also handled here.
 |            Bookmarks are stored in a separate Bookmark table.
 |
 |  Packages:  java.sql
 |             java.util.Scanner
 |
 |  Methods:
 |      menu()                    - Displays sub-menu and routes to methods
 |      startConversationPrompt() - Collects input and calls startConversation()
 |      addMessagePrompt()        - Collects input and calls addMessage()
 |      updateFeedbackPrompt()    - Collects input and calls updateFeedback()
 |      startConversation()       - Inserts a new Conversation row
 |      addMessage()              - Inserts a new Message row
 |      updateFeedback()          - Inserts or updates a Feedback row
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
        int userID = Integer.parseInt(scanner.nextLine().trim());      // FK -> ApplicationUser

        System.out.print("Workspace ID: ");
        int workspaceID = Integer.parseInt(scanner.nextLine().trim()); // FK -> Workspace

        System.out.print("Persona ID (-1 for none): ");
        int personaID = Integer.parseInt(scanner.nextLine().trim());   // FK -> Persona, or -1

        System.out.print("Conversation title: ");
        String title = scanner.nextLine().trim();                      // descriptive name for this thread

        int newID = startConversation(conn, userID, workspaceID, personaID, title); // generated conversationID
        if (newID != -1) {
            conn.commit();
        }
    }

    /*-------------------------------------------------------------------------
     | Method: addMessagePrompt
     |
     | Purpose: Collects user input for a new message and delegates to
     |          addMessage(). Verifies the user has not exceeded their tier's
     |          daily message limit before inserting. Commits on success.
     |
     | Pre-condition:  A valid, open database connection is provided.
     |
     | Post-condition: A new Message row is committed to the database
     |                 if the rate limit check passes and input is valid.
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
        int userID = Integer.parseInt(scanner.nextLine().trim());         // FK -> ApplicationUser, needed for rate limit check

        // Verify the user has not exceeded their tier's daily message limit
        // NOTE: assumes UserManager.checkRateLimit(conn, userID) returns true if within limit
        if (!UserManager.checkRateLimit(conn, userID)) {
            System.out.println("Message limit reached for this user's subscription tier.");
            return;
        }

        System.out.print("Conversation ID: ");
        int conversationID = Integer.parseInt(scanner.nextLine().trim()); // FK -> Conversation

        System.out.print("Role (user / assistant): ");
        String role = scanner.nextLine().trim();                          // sender type

        System.out.print("Message content: ");
        String content = scanner.nextLine().trim();                       // body text of the message

        int newID = addMessage(conn, conversationID, role, content);      // generated messageID
        if (newID != -1) {
            conn.commit();
        }
    }

    /*-------------------------------------------------------------------------
     | Method: updateFeedbackPrompt
     |
     | Purpose: Collects user input for message feedback and delegates to
     |          updateFeedback(). Converts the user's "Thumbs Up" / "Thumbs
     |          Down" input to a numeric rating (1 / 0) before delegating.
     |          Commits the transaction on success.
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
        int messageID = Integer.parseInt(scanner.nextLine().trim());      // FK -> Message

        System.out.print("Conversation ID: ");
        int conversationID = Integer.parseInt(scanner.nextLine().trim()); // FK -> Message (composite PK)

        // Verify the message exists before attempting feedback
        String checkSql = "SELECT COUNT(*) FROM asbarnica.Message WHERE messageID = ? AND conversationID = ?";
        PreparedStatement check = conn.prepareStatement(checkSql);
        check.setInt(1, messageID);
        check.setInt(2, conversationID);
        ResultSet rs = check.executeQuery();
        rs.next();
        if (rs.getInt(1) == 0) {
            System.out.println("No message found with messageID=" + messageID + " and conversationID=" + conversationID);
            check.close();
            return;
        }
        check.close();

        System.out.print("Rating (Thumbs Up / Thumbs Down): ");
        String ratingInput = scanner.nextLine().trim();                   // raw rating string from user
        int rating;
        if (ratingInput.equalsIgnoreCase("Thumbs Up")) {
            rating = 1;                                                   // stored as 1 in DB
        } else if (ratingInput.equalsIgnoreCase("Thumbs Down")) {
            rating = 0;                                                   // stored as 0 in DB
        } else {
            System.out.println("Invalid rating. Please enter 'Thumbs Up' or 'Thumbs Down'.");
            return;
        }

        System.out.print("Feedback text (press Enter to skip): ");
        String feedbackText = scanner.nextLine().trim();                  // optional written feedback
        if (feedbackText.isEmpty()) {
            feedbackText = null;                                          // treat empty input as no feedback text
        }

        boolean success = updateFeedback(conn, messageID, conversationID, rating, feedbackText);
        if (success) {
            conn.commit();
        }
    }

    /*-------------------------------------------------------------------------
     | Method: startConversation
     |
     | Purpose: Creates a new conversation record in the database for a given
     |          user and workspace. Inserts a row into the Conversation table
     |          with activeStatus set to 1 (active) and returns the generated
     |          conversationID.
     |
     | Pre-condition:  A valid, open database connection is provided. userID
     |                 and workspaceID must reference existing rows in their
     |                 respective tables. personaID must reference an existing
     |                 Persona row, or be -1 for no persona.
     |
     | Post-condition: A new Conversation row is inserted into the database.
     |
     | Parameters:
     |      conn        (in) - open Oracle database connection
     |      userID      (in) - ID of the user starting the conversation
     |      workspaceID (in) - ID of the workspace the conversation belongs to
     |      personaID   (in) - ID of the persona to attach, or -1 for none
     |      title       (in) - human-readable title for the conversation
     |
     | Returns:  the generated conversationID, or -1 on failure
     *-----------------------------------------------------------------------*/
    public static int startConversation(Connection conn, int userID, int workspaceID,
                                        int personaID, String title) throws SQLException {
        String sql = "INSERT INTO asbarnica.Conversation (conversationID, userID, workspaceID, "
                   + "personaID, title, creationDate, activeStatus) "
                   + "VALUES (SEQ_CONVERSATION.NEXTVAL, ?, ?, ?, ?, SYSDATE, 1)";

        PreparedStatement pstmt = conn.prepareStatement(sql, new String[]{"conversationID"});
        pstmt.setInt(1, userID);
        pstmt.setInt(2, workspaceID);

        if (personaID < 0) {
            pstmt.setNull(3, Types.INTEGER); // no persona attached to this conversation
        } else {
            pstmt.setInt(3, personaID);      // FK -> Persona
        }

        pstmt.setString(4, title);
        pstmt.executeUpdate();

        ResultSet rs = pstmt.getGeneratedKeys();                   // holds the auto-generated conversationID
        if (rs.next()) {
            int newID = rs.getInt(1);                              // the newly created conversation's PK
            System.out.println("Conversation started with ID: " + newID);
            pstmt.close();
            return newID;
        }

        pstmt.close();
        return -1;
    }

    /*-------------------------------------------------------------------------
     | Method: addMessage
     |
     | Purpose: Inserts a new message into an existing conversation. The role
     |          field distinguishes between 'user' and 'assistant' messages.
     |          Note: bookmarking is handled separately via the Bookmark table
     |          and is not part of the Message row itself.
     |
     | Pre-condition:  A valid, open database connection is provided.
     |                 conversationID must reference an existing Conversation row.
     |
     | Post-condition: A new Message row is inserted into the database.
     |
     | Parameters:
     |      conn           (in) - open Oracle database connection
     |      conversationID (in) - ID of the conversation this message belongs to
     |      role           (in) - "user" or "assistant"
     |      content        (in) - text body of the message
     |
     | Returns:  the generated messageID, or -1 on failure
     *-----------------------------------------------------------------------*/
    public static int addMessage(Connection conn, int conversationID,
                                 String role, String content) throws SQLException {
        String sql = "INSERT INTO asbarnica.Message (messageID, conversationID, role, content, timeSent) "
                   + "VALUES (SEQ_MESSAGE.NEXTVAL, ?, ?, ?, SYSTIMESTAMP)";

        PreparedStatement pstmt = conn.prepareStatement(sql, new String[]{"messageID"});
        pstmt.setInt(1, conversationID);
        pstmt.setString(2, role);
        pstmt.setString(3, content);
        pstmt.executeUpdate();

        ResultSet rs = pstmt.getGeneratedKeys();                   // holds the auto-generated messageID
        if (rs.next()) {
            int newID = rs.getInt(1);                              // the newly created message's PK
            System.out.println("Message added with ID: " + newID);
            pstmt.close();
            return newID;
        }

        pstmt.close();
        return -1;
    }

    /*-------------------------------------------------------------------------
     | Method: updateFeedback
     |
     | Purpose: Inserts or updates a feedback record for a given message.
     |          Attempts an UPDATE first; if no existing row is found for the
     |          given messageID and conversationID, falls through to INSERT.
     |          feedbackText is optional and may be null.
     |
     | Pre-condition:  A valid, open database connection is provided.
     |                 messageID and conversationID must together reference
     |                 an existing Message row (composite PK). rating must
     |                 be 1 (Thumbs Up) or 0 (Thumbs Down).
     |
     | Post-condition: The Feedback table reflects the latest rating and
     |                 feedback text for the given message.
     |
     | Parameters:
     |      conn           (in) - open Oracle database connection
     |      messageID      (in) - message component of the Message composite PK
     |      conversationID (in) - conversation component of the Message composite PK
     |      rating         (in) - 1 for Thumbs Up, 0 for Thumbs Down
     |      feedbackText   (in) - optional written feedback (may be null)
     |
     | Returns:  true if feedback was saved successfully, false otherwise
     *-----------------------------------------------------------------------*/
    public static boolean updateFeedback(Connection conn, int messageID,
                                         int conversationID, int rating,
                                         String feedbackText) throws SQLException {

        // Try to update an existing feedback row first
        String updateSql = "UPDATE asbarnica.Feedback SET rating = ?, feedbackText = ?, "
                         + "timeSubmitted = SYSTIMESTAMP "
                         + "WHERE messageID = ? AND conversationID = ?";

        PreparedStatement pstmt = conn.prepareStatement(updateSql);
        pstmt.setInt(1, rating);
        if (feedbackText == null) {
            pstmt.setNull(2, Types.VARCHAR);  // optional field — no feedback text provided
        } else {
            pstmt.setString(2, feedbackText);
        }
        pstmt.setInt(3, messageID);
        pstmt.setInt(4, conversationID);

        int rows = pstmt.executeUpdate();
        pstmt.close();

        if (rows == 1) {
            System.out.println("Feedback updated for message ID: " + messageID);
            return true;
        }

        // No existing row — insert a new feedback record
        String insertSql = "INSERT INTO asbarnica.Feedback "
                         + "(feedbackID, messageID, conversationID, rating, feedbackText, timeSubmitted) "
                         + "VALUES (SEQ_FEEDBACK.NEXTVAL, ?, ?, ?, ?, SYSTIMESTAMP)";

        pstmt = conn.prepareStatement(insertSql);
        pstmt.setInt(1, messageID);
        pstmt.setInt(2, conversationID);
        pstmt.setInt(3, rating);
        if (feedbackText == null) {
            pstmt.setNull(4, Types.VARCHAR);  // optional field — no feedback text provided
        } else {
            pstmt.setString(4, feedbackText);
        }

        rows = pstmt.executeUpdate();
        pstmt.close();

        if (rows == 1) {
            System.out.println("Feedback added for message ID: " + messageID);
            return true;
        }

        return false;
    }
}
