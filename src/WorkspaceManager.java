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
 |  Class Name:  WorkspaceManager
 |
 |  Purpose:  Handles all workspace-related operations for the LLM platform
 |            (Functionality 3). Allows users to create workspaces, modify
 |            workspace names, and move conversations into workspaces.
 |            Before moving a conversation, the system verifies that the
 |            user is a member of the target workspace.
 |
 |  Methods:
 |      menu()             - Displays workspace options and routes to methods
 |      createWorkspace()  - Inserts a new workspace and adds creator as member
 |      modifyWorkspace()  - Updates the name of an existing workspace
 |      moveConversation() - Moves a conversation into a workspace (with check)
 |      nextVal()          - Helper: gets the next value from an Oracle sequence
 |
 *---------------------------------------------------------------------------*/

import java.sql.*;
import java.util.Scanner;

public class WorkspaceManager {

    /*-------------------------------------------------------------------------
     | Method: menu
     |
     | Purpose: Displays the workspace management sub-menu and routes the
     |          user's choice to the appropriate method.
     |
     | Pre-condition:  A valid, open database connection is provided.
     |
     | Post-condition: One workspace operation is performed based on user input.
     |
     | Parameters:
     |      conn     - open Oracle database connection
     |      scanner  - Scanner object for reading user input
     |
     | Returns: Nothing
     *-----------------------------------------------------------------------*/
    public static void menu(Connection conn, Scanner scanner) throws SQLException {
        System.out.println("\n--- Workspace Management ---");
        System.out.println("1. Create workspace");
        System.out.println("2. Modify workspace name");
        System.out.println("3. Move conversation to workspace");
        System.out.print("Choice: ");

        String choice = scanner.nextLine().trim(); // user's menu selection

        switch (choice) {
            case "1": createWorkspace(conn, scanner);  break;
            case "2": modifyWorkspace(conn, scanner);  break;
            case "3": moveConversation(conn, scanner); break;
            default:  System.out.println("Invalid choice.");
        }
    }

    /*-------------------------------------------------------------------------
     | Method: createWorkspace
     |
     | Purpose: Inserts a new Workspace row into the database. The creator is
     |          automatically added to WorkspaceMembership so they can
     |          immediately use and share the workspace.
     |
     | Pre-condition:  The provided creatorID must exist in ApplicationUser.
     |
     | Post-condition: A new Workspace row and a WorkspaceMembership row for
     |                 the creator are committed to the database.
     |
     | Parameters:
     |      conn     - open Oracle database connection
     |      scanner  - Scanner object for reading user input
     |
     | Returns: Nothing
     *-----------------------------------------------------------------------*/
    private static void createWorkspace(Connection conn, Scanner scanner) throws SQLException {
        System.out.print("Creator User ID: ");
        int creatorID = Integer.parseInt(scanner.nextLine().trim()); // FK -> ApplicationUser

        System.out.print("Workspace name: ");
        String workspaceName = scanner.nextLine().trim(); // display name of the workspace

        System.out.print("Private? (1 = yes, 0 = no): ");
        int privateStatus = Integer.parseInt(scanner.nextLine().trim()); // 1 = private, 0 = public

        int workspaceID = nextVal(conn, "SEQ_WORKSPACE"); // generated PK for new workspace

        String insertWorkspace = "INSERT INTO Workspace " +
                                 "(workspaceID, creatorID, \"name\", privateStatus, creationDate) " +
                                 "VALUES (?, ?, ?, ?, SYSDATE)";
        PreparedStatement pstmt = conn.prepareStatement(insertWorkspace);
        pstmt.setInt(1, workspaceID);
        pstmt.setInt(2, creatorID);
        pstmt.setString(3, workspaceName);
        pstmt.setInt(4, privateStatus);
        pstmt.executeUpdate();
        pstmt.close();

        // Creator is automatically added as a member of the workspace they created
        String insertMembership = "INSERT INTO WorkspaceMembership VALUES (?, ?, SYSDATE)";
        PreparedStatement pstmt2 = conn.prepareStatement(insertMembership);
        pstmt2.setInt(1, creatorID);
        pstmt2.setInt(2, workspaceID);
        pstmt2.executeUpdate();
        pstmt2.close();

        conn.commit();
        System.out.println("Workspace " + workspaceID + " created. Creator added as member.");
    }

    /*-------------------------------------------------------------------------
     | Method: modifyWorkspace
     |
     | Purpose: Updates the name of an existing workspace identified by its ID.
     |
     | Pre-condition:  The provided workspaceID must exist in the Workspace table.
     |
     | Post-condition: The workspace's name is updated in the database.
     |
     | Parameters:
     |      conn     - open Oracle database connection
     |      scanner  - Scanner object for reading user input
     |
     | Returns: Nothing
     *-----------------------------------------------------------------------*/
    private static void modifyWorkspace(Connection conn, Scanner scanner) throws SQLException {
        System.out.print("Workspace ID: ");
        int workspaceID = Integer.parseInt(scanner.nextLine().trim()); // PK of workspace to update

        System.out.print("New name: ");
        String newName = scanner.nextLine().trim(); // replacement workspace name

        String updateSql = "UPDATE Workspace SET \"name\" = ? WHERE workspaceID = ?";
        PreparedStatement pstmt = conn.prepareStatement(updateSql);
        pstmt.setString(1, newName);
        pstmt.setInt(2, workspaceID);
        int rowsUpdated = pstmt.executeUpdate(); // number of rows affected by the update
        pstmt.close();

        conn.commit();

        if (rowsUpdated > 0)
            System.out.println("Workspace updated.");
        else
            System.out.println("Workspace not found.");
    }

    /*-------------------------------------------------------------------------
     | Method: moveConversation
     |
     | Purpose: Moves a conversation into a workspace. The user must be a 
     |          member of the target workspace before the move is allowed. 
     |
     | Pre-condition:  The provided userID, conversationID, and workspaceID
     |                 must all exist in their respective tables.
     |
     | Post-condition: If the user is a member of the workspace, the
     |                 conversation's workspaceID is updated. Otherwise,
     |                 no changes are made to the database.
     |
     | Parameters:
     |      conn     - open Oracle database connection
     |      scanner  - Scanner object for reading user input
     |
     | Returns: Nothing 
     *-----------------------------------------------------------------------*/
    private static void moveConversation(Connection conn, Scanner scanner) throws SQLException {
        System.out.print("User ID: ");
        int userID = Integer.parseInt(scanner.nextLine().trim()); // user requesting the move

        System.out.print("Conversation ID: ");
        int conversationID = Integer.parseInt(scanner.nextLine().trim()); // conversation to move

        System.out.print("Target Workspace ID: ");
        int workspaceID = Integer.parseInt(scanner.nextLine().trim()); // destination workspace

        // Verify the user belongs to the target workspace before allowing the move
        String membershipCheck = "SELECT COUNT(*) FROM WorkspaceMembership " +
                                 "WHERE userID = ? AND workspaceID = ?";
        PreparedStatement checkStmt = conn.prepareStatement(membershipCheck);
        checkStmt.setInt(1, userID);
        checkStmt.setInt(2, workspaceID);
        ResultSet checkResult = checkStmt.executeQuery();
        checkResult.next();
        int memberCount = checkResult.getInt(1); // 1 = is a member, 0 = not a member
        checkResult.close();
        checkStmt.close();

        if (memberCount == 0) {
            System.out.println("Cannot move: user " + userID +
                               " is not a member of workspace " + workspaceID + ".");
            return;
        }

        // Membership confirmed: update the conversation's workspace
        String updateSql = "UPDATE Conversation SET workspaceID = ? WHERE conversationID = ?";
        PreparedStatement pstmt = conn.prepareStatement(updateSql);
        pstmt.setInt(1, workspaceID);
        pstmt.setInt(2, conversationID);
        int rowsUpdated = pstmt.executeUpdate(); // number of rows affected
        pstmt.close();

        conn.commit();

        if (rowsUpdated > 0)
            System.out.println("Conversation " + conversationID +
                               " moved to workspace " + workspaceID + ".");
        else
            System.out.println("Conversation not found.");
    }

    /*-------------------------------------------------------------------------
     | Method: nextVal
     |
     | Purpose: Retrieves the next value from an Oracle sequence. Used to
     |          generate primary keys for new rows.
     |
     | Pre-condition:  The named sequence must exist in the database.
     |
     | Post-condition: The sequence counter is incremented by 1.
     |
     | Parameters:
     |      conn          - open Oracle database connection
     |      sequenceName  - name of the Oracle sequence to query
     |
     | Returns: int - the next available integer value from the sequence
     *-----------------------------------------------------------------------*/
    static int nextVal(Connection conn, String sequenceName) throws SQLException {
        String sql = "SELECT " + sequenceName + ".NEXTVAL FROM DUAL";
        Statement stmt = conn.createStatement();
        ResultSet result = stmt.executeQuery(sql);
        result.next();
        int nextID = result.getInt(1); // the generated primary key value
        result.close();
        stmt.close();
        return nextID;
    }
}
