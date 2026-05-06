/*-----------------------------------------------------------------------------
 |    Assignment:  Program #4 - Database Design and Implementation
 |       Authors:  Gabriel I. Hernandez (gabehernandez07@arizona.edu)
 |                 Andrew Barnica (giallanza1@arizona.edu)
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
 |  Class Name:  TicketManager
 |
 |  Purpose:  Handles the full support ticket lifecycle for the LLM platform
 |            (Functionality 8). Tickets are created in an unassigned, Open
 |            state. They can then be assigned to a support agent, and then
 |            resolved or escalated with a recorded resolution time.
 |
 |  Methods:
 |      menu()          - Displays ticket options and routes to methods
 |      createTicket()  - Opens a new support ticket for a user
 |      assignTicket()  - Assigns an existing ticket to a support agent
 |      resolveTicket() - Closes a ticket with an outcome and resolution days
 |
 *---------------------------------------------------------------------------*/

import java.sql.*;
import java.util.Scanner;

public class TicketManager {

    /*-------------------------------------------------------------------------
     | Method: menu
     |
     | Purpose: Displays the support ticket sub-menu and routes the user's
     |          choice to the appropriate method.
     |
     | Pre-condition:  A valid, open database connection is provided.
     |
     | Post-condition: One ticket operation is performed based on user input.
     |
     | Parameters:
     |      conn     - open Oracle database connection
     |      scanner  - Scanner object for reading user input
     |
     | Returns: Nothing 
     *-----------------------------------------------------------------------*/
    public static void menu(Connection conn, Scanner scanner) throws SQLException {
        System.out.println("\n--- Support Tickets ---");
        System.out.println("1. Create ticket");
        System.out.println("2. Assign ticket to agent");
        System.out.println("3. Resolve or escalate ticket");
        System.out.print("Choice: ");

        String choice = scanner.nextLine().trim(); // user's menu selection

        switch (choice) {
            case "1": createTicket(conn, scanner);  break;
            case "2": assignTicket(conn, scanner);  break;
            case "3": resolveTicket(conn, scanner); break;
            default:  System.out.println("Invalid choice.");
        }
    }

    /*-------------------------------------------------------------------------
     | Method: createTicket
     |
     | Purpose: Opens a new support ticket for a given user.
     |
     | Pre-condition:  The provided userID must exist in ApplicationUser.
     |
     | Post-condition: A new SupportTicket row is inserted and committed
     |                 with agentID = NULL and outcome = 'Open'.
     |
     | Parameters:
     |      conn     - open Oracle database connection
     |      scanner  - Scanner object for reading user input
     |
     | Returns: Nothing
     *-----------------------------------------------------------------------*/
    private static void createTicket(Connection conn, Scanner scanner) throws SQLException {
        System.out.print("User ID: ");
        int userID = Integer.parseInt(scanner.nextLine().trim()); // FK -> ApplicationUser

        System.out.print("Topic (e.g. Billing, Model Error): ");
        String topic = scanner.nextLine().trim(); // short description of the issue

        int ticketID = WorkspaceManager.nextVal(conn, "SEQ_TICKET"); // generated PK

        // agentID starts NULL, resolutionDays NULL, outcome 'Open'
        String insertSql = "INSERT INTO giallanza1.SupportTicket VALUES (?, ?, NULL, ?, SYSDATE, NULL, 'Open')";
        PreparedStatement pstmt = conn.prepareStatement(insertSql);
        pstmt.setInt(1, ticketID);
        pstmt.setInt(2, userID);
        pstmt.setString(3, topic);
        pstmt.executeUpdate();
        pstmt.close();

        conn.commit();
        System.out.println("Ticket " + ticketID + " created (unassigned, Open).");
    }

    /*-------------------------------------------------------------------------
     | Method: assignTicket
     |
     | Purpose: Assigns an existing support ticket to a support agent by
     |          updating the agentID field on the ticket.
     |
     | Pre-condition:  The provided ticketID must exist in SupportTicket.
     |                 The provided agentID must exist in SupportAgent.
     |
     | Post-condition: The ticket's agentID is updated in the database.
     |
     | Parameters:
     |      conn     - open Oracle database connection
     |      scanner  - Scanner object for reading user input
     |
     | Returns: Nothing 
     *-----------------------------------------------------------------------*/
    private static void assignTicket(Connection conn, Scanner scanner) throws SQLException {
        System.out.print("Ticket ID: ");
        int ticketID = Integer.parseInt(scanner.nextLine().trim()); // PK of ticket to assign

        System.out.print("Agent ID: ");
        int agentID = Integer.parseInt(scanner.nextLine().trim()); // FK -> SupportAgent

        String updateSql = "UPDATE giallanza1.SupportTicket SET agentID = ? WHERE ticketID = ?";
        PreparedStatement pstmt = conn.prepareStatement(updateSql);
        pstmt.setInt(1, agentID);
        pstmt.setInt(2, ticketID);
        int rowsUpdated = pstmt.executeUpdate(); // number of rows affected
        pstmt.close();

        conn.commit();

        if (rowsUpdated > 0)
            System.out.println("Ticket " + ticketID + " assigned to agent " + agentID + ".");
        else
            System.out.println("Ticket not found.");
    }

    /*-------------------------------------------------------------------------
     | Method: resolveTicket
     |
     | Purpose: Closes a support ticket by setting its final outcome
     |          (Resolved or Escalated) and recording how many days it
     |          took to resolve.
     |
     | Pre-condition:  The provided ticketID must exist in SupportTicket.
     |                 The outcome must be either "Resolved" or "Escalated".
     |
     | Post-condition: The ticket's outcome and resolutionDays are updated
     |                 and committed to the database.
     |
     | Parameters:
     |      conn     - open Oracle database connection
     |      scanner  - Scanner object for reading user input
     |
     | Returns: Nothing
     *-----------------------------------------------------------------------*/
    private static void resolveTicket(Connection conn, Scanner scanner) throws SQLException {
        System.out.print("Ticket ID: ");
        int ticketID = Integer.parseInt(scanner.nextLine().trim()); // PK of ticket to close

        System.out.print("Outcome (Resolved / Escalated): ");
        String outcome = scanner.nextLine().trim(); // final status of the ticket

        System.out.print("Number of days to resolve: ");
        int resolutionDays = Integer.parseInt(scanner.nextLine().trim()); // duration of resolution

        String updateSql = "UPDATE giallanza1.SupportTicket " +
                           "SET outcome = ?, resolutionDays = ? " +
                           "WHERE ticketID = ?";
        PreparedStatement pstmt = conn.prepareStatement(updateSql);
        pstmt.setString(1, outcome);
        pstmt.setInt(2, resolutionDays);
        pstmt.setInt(3, ticketID);
        int rowsUpdated = pstmt.executeUpdate(); // number of rows affected
        pstmt.close();

        conn.commit();

        if (rowsUpdated > 0)
            System.out.println("Ticket " + ticketID + ": " + outcome +
                               " in " + resolutionDays + " day(s).");
        else
            System.out.println("Ticket not found.");
    }
}
