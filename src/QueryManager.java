/*-----------------------------------------------------------------------------
 |    Assignment:  Program #4 - Database Design and Implementation
 |       Authors:  Gabriel I. Hernandez (gabehernandez07@arizona.edu)
 |                 Andrew Barnica (asbarnica@arizona.edu)
 |                 Tom Giallanza (giallanza1@arizona.edu)
 |                 Helena Musial (helenamusial@arizona.edu)
 |
 |        Course:  CSC 460 (Database Design)
 |    Instructor:  L. McCann
 | Sect. Leaders:  Jianwei Shen, Muhammad Bilal
 |      Due Date:  May 5th, 2026, at the beginning of class
 |
 |      Language:  Java 25
 |      Packages:  java.sql, java.util
 |
 +-----------------------------------------------------------------------------
 |
 |  Class Name:  QueryManager
 |
 |  Purpose:  Handles all four queries for the LLM platform.
 |            Each query joins multiple tables and displays formatted results
 |            to the user. 
 |
 |  Queries:
 |      queryBookmarkedMessages()     - Q1: all bookmarked messages for a user
 |      queryUnpaidInvoices()         - Q2: users with unpaid invoices
 |      queryMostHelpfulPersona()     - Q3: persona with highest % Thumbs Up
 |      queryPersonaMessageActivity() - Q4: message breakdown for a persona
 |
 *---------------------------------------------------------------------------*/

import java.sql.*;
import java.util.Scanner;

public class QueryManager {

    /*-------------------------------------------------------------------------
     | Method: menu
     |
     | Purpose: Displays the query sub-menu and routes the user's choice
     |          to the right query method.
     |
     | Pre-condition:  A valid, open database connection is provided.
     |
     | Post-condition: One query is executed and results are printed.
     |
     | Parameters:
     |      conn     - open Oracle database connection
     |      scanner  - Scanner object for reading user input
     |
     | Returns: Nothing
     *-----------------------------------------------------------------------*/
    public static void menu(Connection conn, Scanner scanner) throws SQLException {
        System.out.println("\n--- Queries ---");
        System.out.println("1. Bookmarked messages for a user");
        System.out.println("2. Users with unpaid invoices");
        System.out.println("3. Most helpful persona");
        System.out.println("4. Message activity for a persona");
        System.out.print("Choice: ");

        String choice = scanner.nextLine().trim(); // user's menu selection

        switch (choice) {
            case "1": queryBookmarkedMessages(conn, scanner);     break;
            case "2": queryUnpaidInvoices(conn);                  break;
            case "3": queryMostHelpfulPersona(conn);              break;
            case "4": queryPersonaMessageActivity(conn, scanner); break;
            default:  System.out.println("Invalid choice.");
        }
    }

    /*-------------------------------------------------------------------------
     | Method: queryBookmarkedMessages
     |
     | Purpose: Lists all messages bookmarked by a specific user, including
     |          the conversation title and bookmark timestamp.
     |          Tables joined: Bookmark -> Message -> Conversation
     |
     | Pre-condition:  The provided userID must exist in ApplicationUser.
     |
     | Post-condition: Results are printed to the screen. No DB changes made.
     |
     | Parameters:
     |      conn     - open Oracle database connection
     |      scanner  - Scanner object for reading user input (userID)
     |
     | Returns: Nothing 
     *-----------------------------------------------------------------------*/
    private static void queryBookmarkedMessages(Connection conn, Scanner scanner)
            throws SQLException {

        System.out.print("Enter User ID: ");
        int userID = Integer.parseInt(scanner.nextLine().trim()); // user to look up bookmarks for

        String sql = "SELECT c.title AS conversationTitle, " +
                     "       m.content AS messageContent, " +
                     "       bk.timeBookmarked " +
                     "FROM asbarnica.Bookmark bk " +
                     "JOIN asbarnica.Message m      ON bk.messageID     = m.messageID " +
                     "JOIN asbarnica.Conversation c ON m.conversationID = c.conversationID " +
                     "WHERE bk.userID = ? " +
                     "ORDER BY bk.timeBookmarked DESC";

        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, userID);
        ResultSet results = pstmt.executeQuery();

        System.out.println("\n--- Bookmarked Messages for User " + userID + " ---");
        System.out.printf("%-30s %-45s %-25s%n", "Conversation", "Message", "Bookmarked At");
        System.out.println("-".repeat(103));

        boolean foundAny = false; // tracks whether any rows were returned

        while (results.next()) {
            foundAny = true;
            String conversationTitle = results.getString("conversationTitle");
            String messageContent    = results.getString("messageContent");
            String bookmarkedAt      = results.getTimestamp("timeBookmarked").toString();

            if (messageContent != null && messageContent.length() > 42)
                messageContent = messageContent.substring(0, 42) + "...";

            System.out.printf("%-30s %-45s %-25s%n",
                    conversationTitle, messageContent, bookmarkedAt);
        }

        if (!foundAny)
            System.out.println("No bookmarks found for user " + userID + ".");

        results.close();
        pstmt.close();
    }

    /*-------------------------------------------------------------------------
     | Method: queryUnpaidInvoices
     |
     | Purpose: Lists all users who have at least one unpaid invoice, showing
     |          their email, total amount owed, and date of their last
     |          conversation. 
     |          Tables joined: ApplicationUser -> Invoice -> Conversation
     |
     | Pre-condition:  None.
     |
     | Post-condition: Results are printed to the screen. No DB changes made.
     |
     | Parameters:
     |      conn  - open Oracle database connection
     |
     | Returns: Nothing 
     *-----------------------------------------------------------------------*/
    private static void queryUnpaidInvoices(Connection conn) throws SQLException {

        String sql = "SELECT u.email, " +
                     "       SUM(i.amount) AS totalOwed, " +
                     "       MAX(c.creationDate) AS lastConversationDate " +
                     "FROM asbarnica.ApplicationUser u " +
                     "JOIN asbarnica.Invoice i      ON u.userID = i.userID " +
                     "LEFT JOIN asbarnica.Conversation c ON u.userID = c.userID " +
                     "WHERE i.paymentStatus = 0 " +   // 0 = Unpaid
                     "GROUP BY u.userID, u.email " +
                     "ORDER BY totalOwed DESC";

        Statement stmt = conn.createStatement();
        ResultSet results = stmt.executeQuery(sql);

        System.out.println("\n--- Users With Unpaid Invoices ---");
        System.out.printf("%-30s %12s %-20s%n", "Email", "Total Owed", "Last Conversation");
        System.out.println("-".repeat(65));

        boolean foundAny = false; // tracks whether any rows were returned

        while (results.next()) {
            foundAny = true;
            String email           = results.getString("email");
            double totalOwed       = results.getDouble("totalOwed");
            Date   lastConvoDate   = results.getDate("lastConversationDate");

            // Handle users who have invoices but no conversations yet
            String lastConvo;
            if (lastConvoDate == null) {
                lastConvo = "No conversations";
            } else {
                lastConvo = lastConvoDate.toString();
            }

            System.out.printf("%-30s $%11.2f %-20s%n", email, totalOwed, lastConvo);
        }

        if (!foundAny) System.out.println("No users with unpaid invoices.");

        results.close();
        stmt.close();
    }

    /*-------------------------------------------------------------------------
     | Method: queryMostHelpfulPersona
     |
     | Purpose: Finds the persona with the highest percentage of Thumbs Up
     |          feedback across all conversations it was used in.
     |          Tables joined: Persona -> Conversation -> Message -> Feedback
     |
     | Pre-condition:  At least one Feedback row must exist in the database.
     |
     | Post-condition: The top-ranked persona is printed. No DB changes made.
     |
     | Parameters:
     |      conn  - open Oracle database connection
     |
     | Returns: Nothing 
     *-----------------------------------------------------------------------*/
    private static void queryMostHelpfulPersona(Connection conn) throws SQLException {

        // Get every persona that has received feedback, with its total count.
        String totalSql = "SELECT p.name AS personaName, COUNT(f.feedbackID) AS totalFeedback " +
                          "FROM asbarnica.Persona p " +
                          "JOIN asbarnica.Conversation c ON p.personaID     = c.personaID " +
                          "JOIN asbarnica.Message m      ON c.conversationID = m.conversationID " +
                          "JOIN asbarnica.Feedback f     ON m.messageID      = f.messageID " +
                          "GROUP BY p.personaID, p.name";

        // For a specific persona, count only the Thumbs Up ratings.
        String thumbsUpSql = "SELECT COUNT(f.feedbackID) AS thumbsUpCount " +
                             "FROM asbarnica.Persona p " +
                             "JOIN asbarnica.Conversation c ON p.personaID     = c.personaID " +
                             "JOIN asbarnica.Message m      ON c.conversationID = m.conversationID " +
                             "JOIN asbarnica.Feedback f     ON m.messageID      = f.messageID " +
                             "WHERE p.name = ? AND f.rating = 1"; // 1 = Thumbs Up

        Statement stmt = conn.createStatement();
        ResultSet allPersonas = stmt.executeQuery(totalSql);

        String bestPersonaName = "";    // name of the persona with the highest approval rate
        double bestPercentage  = -1.0;  // highest approval percentage found so far
        int    bestTotal       = 0;     // total feedback count for the best persona
        int    bestThumbsUp    = 0;     // thumbs up count for the best persona

        // Loop through every persona and calculate its approval percentage
        while (allPersonas.next()) {
            String personaName   = allPersonas.getString("personaName");
            int    totalFeedback = allPersonas.getInt("totalFeedback");

            // Look up how many Thumbs Up this persona received
            PreparedStatement pstmt = conn.prepareStatement(thumbsUpSql);
            pstmt.setString(1, personaName);
            ResultSet thumbsUpResult = pstmt.executeQuery();
            thumbsUpResult.next();
            int thumbsUpCount = thumbsUpResult.getInt("thumbsUpCount");
            thumbsUpResult.close();
            pstmt.close();

            double percentage = (double) thumbsUpCount / totalFeedback * 100.0;

            // Keep track of whichever persona has the highest percentage so far
            if (percentage > bestPercentage) {
                bestPercentage  = percentage;
                bestPersonaName = personaName;
                bestTotal       = totalFeedback;
                bestThumbsUp    = thumbsUpCount;
            }
        }

        allPersonas.close();
        stmt.close();

        System.out.println("\n--- Most Helpful Persona ---");
        if (bestPersonaName.isEmpty()) {
            System.out.println("No feedback data found.");
        } else {
            System.out.println("Persona Name    : " + bestPersonaName);
            System.out.println("Total Feedback  : " + bestTotal);
            System.out.println("Thumbs Up Count : " + bestThumbsUp);
            System.out.printf( "Approval Rate   : %.1f%%%n", bestPercentage);
        }
    }

    /*-------------------------------------------------------------------------
     | Method: queryPersonaMessageActivity
     |
     | Purpose: For a given persona name, shows the total number of messages
     |          sent across all conversations that used that persona, broken
     |          down by role (User vs. Assistant), and identifies the most
     |          active conversation.
     |          Tables joined: Persona -> Conversation -> Message
     |
     | Pre-condition:  A persona matching the provided name must exist.
     |
     | Post-condition: Results are printed to the screen. No DB changes made.
     |
     | Parameters:
     |      conn     - open Oracle database connection
     |      scanner  - Scanner object for reading user input (persona name)
     |
     | Returns: Nothing
     *-----------------------------------------------------------------------*/
    private static void queryPersonaMessageActivity(Connection conn, Scanner scanner)
            throws SQLException {

        System.out.print("Enter Persona Name: ");
        String personaName = scanner.nextLine().trim(); // name of the persona to look up

        // Count all messages for this persona
        String totalSql = "SELECT COUNT(m.messageID) AS totalMessages " +
                          "FROM asbarnica.Persona p " +
                          "JOIN asbarnica.Conversation c ON p.personaID     = c.personaID " +
                          "JOIN asbarnica.Message m      ON c.conversationID = m.conversationID " +
                          "WHERE UPPER(p.name) = UPPER(?)";

        PreparedStatement pstmt1 = conn.prepareStatement(totalSql);
        pstmt1.setString(1, personaName);
        ResultSet totalResult = pstmt1.executeQuery();
        totalResult.next();
        int totalMessages = totalResult.getInt("totalMessages");
        totalResult.close();
        pstmt1.close();

        System.out.println("\n--- Message Activity for Persona: " + personaName + " ---");

        if (totalMessages == 0) {
            System.out.println("No message data found for persona \"" + personaName + "\".");
            return;
        }

        // Count only User-role messages for this persona
        String userSql = "SELECT COUNT(m.messageID) AS userMessages " +
                         "FROM asbarnica.Persona p " +
                         "JOIN asbarnica.Conversation c ON p.personaID     = c.personaID " +
                         "JOIN asbarnica.Message m      ON c.conversationID = m.conversationID " +
                         "WHERE UPPER(p.name) = UPPER(?) AND m.role = 'user'"; 

        PreparedStatement pstmt2 = conn.prepareStatement(userSql);
        pstmt2.setString(1, personaName);
        ResultSet userResult = pstmt2.executeQuery();
        userResult.next();
        int userMessages      = userResult.getInt("userMessages"); // messages sent by users
        int assistantMessages = totalMessages - userMessages; // the rest are Assistant
        userResult.close();
        pstmt2.close();

        System.out.println("Total messages      : " + totalMessages);
        System.out.println("   User messages    : " + userMessages);
        System.out.println("   Assistant msgs   : " + assistantMessages);

        // Find the most active conversation under this persona
        String topConvoSql = "SELECT c.title AS conversationTitle, " +
                             "       COUNT(m.messageID) AS messageCount " +
                             "FROM asbarnica.Persona p " +
                             "JOIN asbarnica.Conversation c ON p.personaID      = c.personaID " +
                             "JOIN asbarnica.Message m      ON c.conversationID  = m.conversationID " +
                             "WHERE UPPER(p.name) = UPPER(?) " +
                             "GROUP BY c.conversationID, c.title " +
                             "ORDER BY messageCount DESC"; // most messages comes first

        PreparedStatement pstmt3 = conn.prepareStatement(topConvoSql);
        pstmt3.setString(1, personaName);
        ResultSet topConvoResult = pstmt3.executeQuery();

        // Returns all conversations sorted by message count.
        // only read the first row to get the most active one.
        if (topConvoResult.next()) {
            System.out.println("Most active convo   : \"" +
                               topConvoResult.getString("conversationTitle") +
                               "\" (" + topConvoResult.getInt("messageCount") + " messages)");
        }

        topConvoResult.close();
        pstmt3.close();
    }
}
