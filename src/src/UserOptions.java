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
 |      Packages:  java.util
 |                 java.sql
 |   Compile/Run:  Run: export CLASSPATH=/usr/lib/oracle/19.8/client64/lib/ojdbc8.jar:${CLASSPATH}
 |                 Compile: javac Prog4.java
 |                 Run:     java Prog4 [username] [password]
 |
 +-----------------------------------------------------------------------------
 |  Description:  This program provides functions that 
 |
 |
 |        Input: fileName.csv - a csv file that (if non-empty) contains data in
 |               the 11 data fields from the given bat cave dataset
 |
 |       Output: fileName.bin - a binary file containing the data from the csv
 |               file.
 |
 |   Techniques: JDBC utilization for interacting with an Oracle SQL DB
 |
 |   Required Features Not Included:  All required features are included.
 |
 |   Known Bugs: None
 |
 *---------------------------------------------------------------------------*/
public class UserOptions {
    public static void options(Connection conn, Scanner scanner) throws SQLException {
        System.out.println("\n--- User Options ---");
        System.out.println("1. Add a user");
        System.out.println("2. Update an existing user's membership tier");
        System.out.println("3. Delete a user");
        System.out.println("4. See a user's rate limit");
        System.out.println("5. Create an invoice");
        System.out.println("6. Update invoice payment status");
        System.out.println("Choice: ");

        String input = scanner.nextInt();
        switch (input) {
            case 1: addUser(conn, scanner); break;
            case 2: updateUserTier(conn, scanner); break;
            case 3: deleteUser(conn, scanner); break;
            case 4: checkRateLimit(conn, scanner); break;
            case 5: generateInvoice(conn, scanner); break;
            case 6: markInvoicePaid(conn, scanner); break;
            default: System.out.println("Invalid Choice: (Enter 1-6)");
        }

    }

    private static void addUser(Connection conn, Scanner scanner) throws SQLException {
        System.out.println("User's name: ");
        String name = scanner.nextLine().trim();

        System.out.println("User's email: ");
        String email = scanner.nextLine().trim();

        System.out.println("User's preferred language: ");
        String lang = scanner.nextLine().trim();

        System.out.println("User's membership tier (Free, Plus, Enterprise): ");
        String tier = scanner.nextLine().trim().toLowerCase();

        int userID = WorkspaceManager.nextVal(conn, "SEQ_USER");

        String query = "INSERT INTO ApplictionUser VALUES (?, ?, ?, SYSDATE, ?, ?)";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setInt(1, userID);
        stmt.setString(2, name);
        stmt.setString(3, email);
        stmt.setString(4, lang);
        stmt.setString(5, tierID);
        stmt.executeUpdate();
        stmt.close();

        System.out.println("\n----------------------------------------------\n");
        System.out.println("User's payment method: ");
        String method = scanner.nextLine().trim();

        System.out.println("User's billing address: ");
        String addr = scanner.nextLine().trim();

        int billingID = WorkspaceManager.nextVal(conn, "SEQ_BILLING");
        
        String query2 = "INSERT INTO BillingProfile VALUES (?, ?, ?, ?)";
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

    private static void updateUserTier(Connection conn, Scanner scanner) throws SQLException {
        System.out.println("User ID: ");
        int userID = scanner.nextInt();

        System.out.println("User " + userID + "'s new membership tier (Free, Plus, Enterprise): ");
        String tier = scanner.nextLine();

        //Fetch tierID from MembershipTier table with matching tier name
        String query = "SELECT tierID FROM membershipTier WHERE tierName = ?";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setString(1, tier);
        ResultSet tierResult = stmt.executeQuery();
        int tierID = tierResult.getInt(1);
        tierResult.close();
        stmt.close();

        String query2 = "UPDATE ApplicationUser SET tierID = ? WHERE userID = ?";
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
    
    private static void deleteUser(Connection conn, Scanner scanner) throws SQLException {
        System.out.println("User ID: ");
        int userID = scanner.nextInt();

        //To delete a user, first check for deletion constraints

        //1. Cannot delete user if they have an unpaid invoice
        String queryInv = "SELECT paymentStatus FROM Invoice WHERE userID = ?";
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
        String querySup = "SELECT outcome FROM Invoice WHERE userID = ?";
        PreparedStatement stmtSup = conn.prepareStatement(querySup);
        stmtSup.setInt(1, userID);
        ResultSet supResult = stmtSup.executeQuery();
        boolean hasOpen = false;

        while (supResult.next()) {
            int outcomeStatus = supResult.getInt("outcome");
            if (outcomeStatus == 0) {
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
        String queryDel = "DELETE FROM ApplicationUser WHERE userID = ?";
        PreparedStatement stmtDel = conn.prepareStatement(queryDel);
        stmtDel.setInt(1, userID);
        int numDeleted = stmtDel.executeQuery();
        if (numDeleted > 0) {
            System.out.println("User " + userID + " deleted.");
        }
        else {
            System.out.println("User deletion failed.");
            return;        
        }
        stmtDel.close();
        //Delete relevant items associated with user () ??

        conn.commit();
    }

    private static void checkRateLimit(Connection conn, Scanner scanner) throws SQLException {
        System.out.println("User ID: ");
        int userID = scanner.nextInt();

        String query = "SELECT mt.maxMessagesPerDay AS rate " + 
                       "FROM MembershipTier mt " + 
                       "JOIN ApplicationUser au " +
                       "ON mt.userID = au.userID " +
                       "WHERE au.userID = ?";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setInt(1, userID);
        ResultSet results = stmt.executeQuery();

        System.out.println("\n--- Rate Limit for User " + userID + " ---");
        results.next();
        System.out.println(results.getInt("rate") + " messages per day.");

        results.close();
        stmt.close();
    }

    private static void generateInvoice(Connection conn, Scanner scanner) throws SQLException {
        System.out.println("User ID: ");
        int userID = scanner.nextInt();

        int invoiceID = WorkspaceManager.nextVal(conn, "SEQ_INVOICE");

        System.out.println("Invoice Amount: ");
        int amount = scanner.nextInt();

        String query = "INSERT INTO Invoice VALUES (?, ?, ?, SYSDATE, 0)";
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

    private static void markInvoicePaid(Connection conn, Scanner scanner) throws SQLException {
        System.out.println("User ID: ");
        int userID = scanner.nextInt();
        
        System.out.println("Invoice ID: ");
        int invoiceID = scanner.nextInt();

        String query = "UPDATE Invoice SET paymentStatus = 1 " +
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
