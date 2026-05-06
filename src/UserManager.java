import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

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
 |      Packages:  java.util
 |                 java.sql
 |   Compile/Run:  Run: export CLASSPATH=/usr/lib/oracle/19.8/client64/lib/ojdbc8.jar:${CLASSPATH}
 |                 Compile: javac Prog4.java
 |                 Run:     java Prog4 [username] [password]
 |
 +-----------------------------------------------------------------------------
 |  Description:  This program provides functions that, in tandem with functions in other program files
 |              allow the user to interact with the Oracle SQL DB containing client data, and perform tasks
 |              described in the project spec. Functions in this program file are all related to user actions,
 |              including adding users to DB, removing users from DB, creating invoices for users, etc.
 |
 |
 |        Input: User input via scanner.
 |
 |       Output: Text output containing DB data or response messages after executing actions.
 |
 |   Techniques: JDBC utilization for interacting with an Oracle SQL DB
 |
 |   Required Features Not Included:  All required features are included.
 |
 |   Known Bugs: None
 |
 *---------------------------------------------------------------------------*/
public class UserManager {
    /*-------------------------------------------------------------------------
     | Method: menu
     |
     | Purpose: Displays the user action menu and processes the user's
     |          choice to the right query method.
     |
     | Pre-condition:  A valid, open database connection is provided.
     |
     | Post-condition: The action selected by the user is performed
     |                 and accurate output is displayed.
     |
     | Parameters:
     |      conn     - open Oracle database connection
     |      scanner  - Scanner object for reading user input
     |
     | Returns: Nothing
     *-----------------------------------------------------------------------*/
    public static void menu(Connection conn, Scanner scanner) throws SQLException {
        System.out.println("\n--- User Options ---");
        System.out.println("1. Add a user");
        System.out.println("2. Update an existing user's membership tier");
        System.out.println("3. Delete a user");
        System.out.println("Choice: ");

        int input = Integer.parseInt(scanner.nextLine().trim());
        switch (input) {
            case 1: addUser(conn, scanner); break;
            case 2: updateUserTier(conn, scanner); break;
            case 3: deleteUser(conn, scanner); break;
            default: System.out.println("Invalid Choice: (Enter 1-3)");
        }

    }

    /*-------------------------------------------------------------------------
     | Method: subscriptionMenu
     |
     | Purpose: Displays the user subscription menu and processes the user's
     |          choice to the right query method
     |
     | Pre-condition:  A valid, open database connection is provided.
     |
     | Post-condition: The action selected by the user is performed
     |                 and accurate output is displayed.
     |
     | Parameters:
     |      conn     - open Oracle database connection
     |      scanner  - Scanner object for reading user input
     |
     | Returns: Nothing
     *-----------------------------------------------------------------------*/
    public static void subscriptionMenu(Connection conn, Scanner scanner) throws SQLException {
        System.out.println("\n--- Subscription Tracking ---");
        System.out.println("1. Update subscription");
        System.out.println("2. See a user's rate limit");
        System.out.println("Choice: ");
        String input = scanner.nextLine().trim();
        switch (input) {
            case "1": updateUserTier(conn, scanner); break;
            case "2": checkRateLimitPrompt(conn, scanner); break;
            default: System.out.println("Invalid Choice: (Enter 1-2)");
        }
    }

    /*-------------------------------------------------------------------------
     | Method: billingMenu
     |
     | Purpose: Displays the user billing menu and processes the user's
     |          choice to the right query method
     |
     | Pre-condition:  A valid, open database connection is provided.
     |
     | Post-condition: The action selected by the user is performed
     |                 and accurate output is displayed.
     |
     | Parameters:
     |      conn     - open Oracle database connection
     |      scanner  - Scanner object for reading user input
     |
     | Returns: Nothing
     *-----------------------------------------------------------------------*/
    public static void billingMenu(Connection conn, Scanner scanner) throws SQLException {
        System.out.println("\n--- Billing Operations ---");
        System.out.println("1. Generate new Invoice");
        System.out.println("2. Mark Invoice Paid");
        System.out.println("Choice: ");
        String input = scanner.nextLine().trim();
        switch (input) {
            case "1": generateInvoice(conn, scanner); break;
            case "2": markInvoicePaid(conn, scanner); break;
            default: System.out.println("Invalid Choice: (Enter 1-2)");
        }
    }

    /*-------------------------------------------------------------------------
     | Method: addUser
     |
     | Purpose: Adds a user record to the ApplicationUser relation within
     |          the Oracle SQL DB. Asks user to provide relevant information
     |          via the scanner object. A billing profile for the new user is
     |          also created.
     |
     | Pre-condition:  A valid, open database connection is provided.
     |
     | Post-condition: The action selected by the user is performed
     |                 and accurate output is displayed.
     |
     | Parameters:
     |      conn     - open Oracle database connection
     |      scanner  - Scanner object for reading user input
     |
     | Returns: Nothing
     *-----------------------------------------------------------------------*/
    private static void addUser(Connection conn, Scanner scanner) throws SQLException {
        System.out.println("User's name: ");
        String name = scanner.nextLine().trim();

        System.out.println("User's email: ");
        String email = scanner.nextLine().trim();

        System.out.println("User's preferred language: ");
        String lang = scanner.nextLine().trim();

        System.out.println("User's membership tier (Free, Plus, Enterprise): ");
        String tier = scanner.nextLine().trim().toLowerCase();

        String queryTier = "SELECT tierID FROM giallanza1.membershipTier WHERE name = ?";
        PreparedStatement stmtTier = conn.prepareStatement(queryTier);
        stmtTier.setString(1, tier);
        ResultSet tierResult = stmtTier.executeQuery();
        tierResult.next();
        int tierID = tierResult.getInt(1);
        tierResult.close();
        stmtTier.close();


        int userID = WorkspaceManager.nextVal(conn, "SEQ_USER");

        String query = "INSERT INTO giallanza1.ApplicationUser VALUES (?, ?, ?, ?, SYSDATE, ?)";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setInt(1, userID);
        stmt.setInt(2, tierID);
        stmt.setString(3, name);
        stmt.setString(4, email);
        stmt.setString(5, lang);
        stmt.executeUpdate();
        stmt.close();

        System.out.println("\n----------------------------------------------\n");
        System.out.println("User's payment method: ");
        String method = scanner.nextLine().trim();

        System.out.println("User's billing address: ");
        String addr = scanner.nextLine().trim();

        int billingID = WorkspaceManager.nextVal(conn, "SEQ_BILLING");

        String query2 = "INSERT INTO giallanza1.BillingProfile VALUES (?, ?, ?, ?)";
        PreparedStatement stmt2 = conn.prepareStatement(query2);
        stmt2.setInt(1, billingID);
        stmt2.setInt(2, userID);
        stmt2.setString(3, method);
        stmt2.setString(4, addr);
        stmt2.executeUpdate();
        stmt2.close();

        conn.commit();
        System.out.println("User " + userID + " created. Associated billing information saved.");
    }

    /*-------------------------------------------------------------------------
     | Method: updateUserTier
     |
     | Purpose: Performs an update to the record in the ApplicationUser
     |          relation, being a change to the tierID field corresponding
     |          to the ID of the provided new tier for the user.
     |
     |
     | Pre-condition:  A valid, open database connection is provided.
     |
     | Post-condition: The action selected by the user is performed
     |                 and accurate output is displayed.
     |
     | Parameters:
     |      conn     - open Oracle database connection
     |      scanner  - Scanner object for reading user input
     |
     | Returns: Nothing
     *-----------------------------------------------------------------------*/
    private static void updateUserTier(Connection conn, Scanner scanner) throws SQLException {
        System.out.println("User ID: ");
        int userID = scanner.nextInt();
        scanner.nextLine();
        System.out.println("User " + userID + "'s new membership tier (Free, Plus, Enterprise): ");
        String tier = scanner.nextLine().trim().toLowerCase();

        //Fetch tierID from MembershipTier table with matching tier name
        String query = "SELECT tierID FROM giallanza1.MembershipTier WHERE name = ?";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setString(1, tier);
        ResultSet tierResult = stmt.executeQuery();
        tierResult.next();
        int tierID = tierResult.getInt(1);
        tierResult.close();
        stmt.close();

        String query2 = "UPDATE giallanza1.ApplicationUser SET tierID = ? WHERE userID = ?";
        PreparedStatement stmt2 = conn.prepareStatement(query2);
        stmt2.setInt(1, tierID);
        stmt2.setInt(2, userID);
        int changesMade = stmt2.executeUpdate();
        stmt2.close();

        conn.commit();
        if (changesMade > 0) {
            System.out.println("User " + userID + "'s membership tier updated.");
        }
        else {
            System.out.println("User field update failed.");
        }
    }

    /*-------------------------------------------------------------------------
     | Method: deleteUser
     |
     | Purpose: Removes a user's record from the ApplicationUser relation,
     |          pending passes of two deletion conditions: 0 unpaid invoices
     |          and 0 open support tickets.
     |
     |
     | Pre-condition:  A valid, open database connection is provided.
     |
     | Post-condition: The action selected by the user is performed
     |                 and accurate output is displayed.
     |
     | Parameters:
     |      conn     - open Oracle database connection
     |      scanner  - Scanner object for reading user input
     |
     | Returns: Nothing
     *-----------------------------------------------------------------------*/
    private static void deleteUser(Connection conn, Scanner scanner) throws SQLException {
        System.out.println("User ID: ");
        int userID = scanner.nextInt();

        //To delete a user, first check for deletion constraints

        //1. Cannot delete user if they have an unpaid invoice
        String queryInv = "SELECT paymentStatus FROM giallanza1.Invoice WHERE userID = ?";
        PreparedStatement stmtInv = conn.prepareStatement(queryInv);
        stmtInv.setInt(1, userID);
        ResultSet invResult = stmtInv.executeQuery();
        boolean hasUnpaid = false;

        while (invResult.next()) {
            int status = invResult.getInt("paymentStatus");
            if (status == 0) {
                hasUnpaid = true;
                break;
            }
        }

        invResult.close();
        stmtInv.close();
        if (hasUnpaid) {
            System.out.println("Could not delete the user " + userID + " due to unpaid invoice(s).");
            return;
        }

        //2. Cannot delete user if they have open support tickets
        String querySup = "SELECT outcome FROM giallanza1.SupportTicket WHERE userID = ?";
        PreparedStatement stmtSup = conn.prepareStatement(querySup);
        stmtSup.setInt(1, userID);
        ResultSet supResult = stmtSup.executeQuery();
        boolean hasOpen = false;

        while (supResult.next()) {
            String outcomeStatus = supResult.getString("outcome");
            if (outcomeStatus.equals("Open")) {
                hasOpen = true;
                break;
            }
        }

        supResult.close();
        stmtSup.close();
        if (hasOpen) {
            System.out.println("Could not delete user " + userID + " due to open support ticket(s).");
            return;
        }

        //Checks passed, delete the user.
        String queryDel = "DELETE FROM giallanza1.ApplicationUser WHERE userID = ?";
        PreparedStatement stmtDel = conn.prepareStatement(queryDel);
        stmtDel.setInt(1, userID);
        int numDeleted = stmtDel.executeUpdate();
        if (numDeleted > 0) {
            System.out.println("User " + userID + " deleted.");
        }
        else {
            System.out.println("User deletion failed.");
            return;
        }
        stmtDel.close();

        conn.commit();
    }

    /*-------------------------------------------------------------------------
     | Method: checkRateLimitPrompt
     |
     | Purpose: Simply requests the user's id and displays the max amount of
     |          messages the user's subscription allots.
     |
     | Pre-condition:  A valid, open database connection is provided.
     |
     | Post-condition: The action selected by the user is performed
     |                 and accurate output is displayed.
     |
     | Parameters:
     |      conn     - open Oracle database connection
     |      scanner  - Scanner object for reading user input
     |
     | Returns: Nothing
     *-----------------------------------------------------------------------*/
    private static void checkRateLimitPrompt(Connection conn, Scanner scanner) throws SQLException {
        System.out.println("User ID: ");
        int userID = Integer.parseInt(scanner.nextLine().trim());
        int msgLimit = getRateLimit(conn, userID);
        if (msgLimit >= 0) {
            System.out.println("Max message limit for user " + userID + " is: " + msgLimit + ".");
        } else {
            System.out.println("User " + userID + " does not exist.");
        }
    }

    /*-------------------------------------------------------------------------
     | Method: checkRateLimit
     |
     | Purpose: Performs a query to the client DB, getting the amount of
     |          messages a user has sent today. This is compared to the max
     |          daily amount of messages for the user's subscription.
     |
     |
     | Pre-condition:  A valid, open database connection is provided.
     |
     | Post-condition: The action selected by the user is performed
     |                 and accurate output is displayed.
     |
     | Parameters:
     |      conn     - open Oracle database connection
     |      userID   - int holding the correct userID
     |
     | Returns:
     |      bool - true if the user still has messages left in the day,
     |             false otherwise
     *-----------------------------------------------------------------------*/
    public static boolean checkRateLimit(Connection conn, int userID) throws SQLException {
        int msgLimit = getRateLimit(conn, userID);
        if (msgLimit < 0) return false; // Check for the user not existing

        // Counting the amount of messages the user has sent today
        String countQuery = "SELECT COUNT(*) " +
                "FROM giallanza1.Message m " +
                "JOIN giallanza1.Conversation c " +
                "ON m.conversationID = c.conversationID " +
                "WHERE c.userID = ? " +
                "AND TRUNC(m.timeSent) = TRUNC(SYSDATE) " +
                "AND m.role = 'user'";

        PreparedStatement stmt = conn.prepareStatement(countQuery);
        stmt.setInt(1, userID);
        ResultSet results = stmt.executeQuery();
        results.next();
        int msgCount = results.getInt(1);
        results.close();
        stmt.close();
        return msgCount < msgLimit;
    }

    /*-------------------------------------------------------------------------
     | Method: getRateLimit
     |
     | Purpose: Performs a query to the client DB, simply looking for the
     |          messages per day limit associated with a user in the DB,
     |          based on their membership tier.
     |
     |
     | Pre-condition:  A valid, open database connection is provided.
     |
     | Post-condition: The action selected by the user is performed
     |                 and accurate output is displayed.
     |
     | Parameters:
     |      conn     - open Oracle database connection
     |      userID   - int holding the correct userID
     |
     | Returns:
     |      limit - the max amount of daily messages a user gets based on their
     |              subscription tier, or -1 if user not found
     *-----------------------------------------------------------------------*/
    private static int getRateLimit(Connection conn, int userID) throws SQLException {
        // Slight change, joined on tierID instead of userID
        String query = "SELECT mt.maxMessagesPerDay AS rate " +
                "FROM giallanza1.MembershipTier mt " +
                "JOIN giallanza1.ApplicationUser au " +
                "ON mt.tierID = au.tierID " +
                "WHERE au.userID = ?";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setInt(1, userID);
        ResultSet res = stmt.executeQuery();
        if (res.next()) {
            int limit = res.getInt("rate");
            res.close();
            return limit;
        }
        res.close();
        return - 1;
    }

    /*-------------------------------------------------------------------------
     | Method: generateInvoice
     |
     | Purpose: Creates a new invoice record in the Invoice relation of the DB
     |          for a provided user. The user is prompted for relevant information
     |          beyond userID to create the invoice.
     |
     |
     | Pre-condition:  A valid, open database connection is provided.
     |
     | Post-condition: The action selected by the user is performed
     |                 and accurate output is displayed.
     |
     | Parameters:
     |      conn     - open Oracle database connection
     |      scanner  - Scanner object for reading user input
     |
     | Returns: Nothing
     *-----------------------------------------------------------------------*/
    private static void generateInvoice(Connection conn, Scanner scanner) throws SQLException {
        System.out.println("User ID: ");
        int userID = scanner.nextInt();

        int invoiceID = WorkspaceManager.nextVal(conn, "SEQ_INVOICE");

        System.out.println("Invoice Amount: ");
        int amount = scanner.nextInt();

        String query = "INSERT INTO giallanza1.Invoice VALUES (?, ?, ?, SYSDATE, 0)";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setInt(1, invoiceID);
        stmt.setInt(2, userID);
        stmt.setInt(3, amount);

        int rowsInserted = stmt.executeUpdate();
        stmt.close();
        if (rowsInserted > 0) {
            System.out.println("Invoice of amount " + amount + " created for user "  + userID + ".");
            System.out.println("Invoice ID: " + invoiceID);
            conn.commit();
        }
        else {
            System.out.println("Invoice generation failed.");
            return;
        }
    }

    /*-------------------------------------------------------------------------
     | Method: markInvoicePaid
     |
     | Purpose: Performs an update to the record in the Invoice
     |          relation, being a change to the paymentStatus field
     |          corresponding to the ID of the provided invoice.
     |
     |
     | Pre-condition:  A valid, open database connection is provided.
     |
     | Post-condition: The action selected by the user is performed
     |                 and accurate output is displayed.
     |
     | Parameters:
     |      conn     - open Oracle database connection
     |      scanner  - Scanner object for reading user input
     |
     | Returns: Nothing
     *-----------------------------------------------------------------------*/
    private static void markInvoicePaid(Connection conn, Scanner scanner) throws SQLException {
        System.out.println("User ID: ");
        int userID = scanner.nextInt();

        System.out.println("Invoice ID: ");
        int invoiceID = scanner.nextInt();

        String query = "UPDATE giallanza1.Invoice SET paymentStatus = 1 " +
                "WHERE userID = ? AND invoiceID = ?";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setInt(1, userID);
        stmt.setInt(2, invoiceID);
        int rowsUpdated = stmt.executeUpdate();

        stmt.close();
        if (rowsUpdated > 0) {
            System.out.println("Invoice #" + invoiceID + " has been marked as paid.");
            conn.commit();
        }
        else {
            System.out.println("Invoice #" + invoiceID + " could not be marked as paid.");
            return;
        }

    }
}
