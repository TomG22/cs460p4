/*-----------------------------------------------------------------------------
 |
 |  Class Name:  PromptManager
 |
 |  Purpose:  Manages reusable prompt templates for the LLM platform
 |            (Functionality 5). Templates allow users to save and reuse
 |            commonly used prompt structures, optionally grouped by
 |            category and shared within a workspace. Templates can be
 |            created and updated in-place without replacing the entire record.
 |
 |  Packages:  java.sql
 |             java.util.Scanner
 |
 |  Methods:
 |      menu()               - Displays sub-menu and routes to methods
 |      addTemplatePrompt()  - Collects input and calls addTemplate()
 |      updateTemplatePrompt() - Collects input and calls updateTemplate()
 |      addTemplate()        - Inserts a new PromptTemplate row
 |      updateTemplate()     - Updates fields on an existing PromptTemplate row
 |
 *---------------------------------------------------------------------------*/

import java.sql.*;
import java.util.Scanner;

public class PromptManager {

    /*-------------------------------------------------------------------------
     | Method: menu
     |
     | Purpose: Displays the prompt library sub-menu and routes the user's
     |          choice to the appropriate method.
     |
     | Pre-condition:  A valid, open database connection is provided.
     |
     | Post-condition: One prompt template operation is performed based on
     |                 user input.
     |
     | Parameters:
     |      conn    (in) - open Oracle database connection
     |      scanner (in) - Scanner object for reading user input
     |
     | Returns: Nothing
     *-----------------------------------------------------------------------*/
    public static void menu(Connection conn, Scanner scanner) throws SQLException {
        System.out.println("\n--- Prompt Library ---");
        System.out.println("1. Add a prompt template");
        System.out.println("2. Update a prompt template");
        System.out.print("Choice: ");

        String choice = scanner.nextLine().trim(); // user's sub-menu selection

        switch (choice) {
            case "1": addTemplatePrompt(conn, scanner);    break;
            case "2": updateTemplatePrompt(conn, scanner); break;
            default:  System.out.println("Invalid choice.");
        }
    }

    /*-------------------------------------------------------------------------
     | Method: addTemplatePrompt
     |
     | Purpose: Collects user input for a new prompt template and delegates
     |          to addTemplate(). Commits the transaction on success.
     |
     | Pre-condition:  A valid, open database connection is provided.
     |
     | Post-condition: A new PromptTemplate row is committed to the database
     |                 if input is valid.
     |
     | Parameters:
     |      conn    (in) - open Oracle database connection
     |      scanner (in) - Scanner object for reading user input
     |
     | Returns: Nothing
     *-----------------------------------------------------------------------*/
    private static void addTemplatePrompt(Connection conn,
                                          Scanner scanner) throws SQLException {
        System.out.print("User ID: ");
        int creatorID = Integer.parseInt(scanner.nextLine().trim()); // FK -> ApplicationUser

        System.out.print("Template title: ");
        String title = scanner.nextLine().trim();                    // short title for the template

        System.out.print("Template content: ");
        String content = scanner.nextLine().trim();                  // full prompt body

        System.out.print("Category: ");
        String category = scanner.nextLine().trim();                 // grouping label (required per schema)

        System.out.print("Make this template private? (y/n): ");
        boolean isPrivate = scanner.nextLine().trim().equalsIgnoreCase("y"); // private or shared flag

        // Only prompt for a workspace ID if the template is being shared
        int workspaceID = -1; // FK -> Workspace, -1 means no workspace (private)
        if (!isPrivate) {
            System.out.print("Workspace ID to share within: ");
            workspaceID = Integer.parseInt(scanner.nextLine().trim());
        }

        int newID = addTemplate(conn, creatorID, title, content, category, isPrivate, workspaceID); // generated templateID
        if (newID != -1) {
            conn.commit();
        }
    }

    /*-------------------------------------------------------------------------
     | Method: updateTemplatePrompt
     |
     | Purpose: Collects user input for a template update and delegates to
     |          updateTemplate(). Only fields the user provides are changed.
     |          Commits the transaction on success.
     |
     | Pre-condition:  A valid, open database connection is provided.
     |
     | Post-condition: The specified PromptTemplate row is updated and
     |                 committed if it exists.
     |
     | Parameters:
     |      conn    (in) - open Oracle database connection
     |      scanner (in) - Scanner object for reading user input
     |
     | Returns: Nothing
     *-----------------------------------------------------------------------*/
    private static void updateTemplatePrompt(Connection conn,
                                              Scanner scanner) throws SQLException {
        System.out.print("Template ID: ");
        int templateID = Integer.parseInt(scanner.nextLine().trim()); // PK of template to update

        // Pressing Enter for any field leaves it unchanged in the database
        System.out.print("New title (press Enter to skip): ");
        String newTitle = scanner.nextLine().trim();                  // replacement title, or empty

        System.out.print("New content (press Enter to skip): ");
        String newContent = scanner.nextLine().trim();                // replacement body, or empty

        System.out.print("New category (press Enter to skip): ");
        String newCategory = scanner.nextLine().trim();               // replacement category, or empty

        if (newTitle.isEmpty())    newTitle    = null;
        if (newContent.isEmpty())  newContent  = null;
        if (newCategory.isEmpty()) newCategory = null;

        boolean success = updateTemplate(conn, templateID, newTitle, newContent, newCategory); // result of update
        if (success) {
            conn.commit();
        }
    }

    /*-------------------------------------------------------------------------
     | Method: addTemplate
     |
     | Purpose: Inserts a new reusable prompt template into the database.
     |          A template may be private to the user or shared within a
     |          specific workspace, controlled by the privateStatus column.
     |
     | Pre-condition:  A valid, open database connection is provided. creatorID
     |                 must reference an existing ApplicationUser row. title,
     |                 content, and category must be non-null and non-empty.
     |                 If isPrivate is false, workspaceID must reference an
     |                 existing Workspace row; otherwise pass -1.
     |
     | Post-condition: A new PromptTemplate row is inserted into the database.
     |
     | Parameters:
     |      conn        (in) - open Oracle database connection
     |      creatorID   (in) - ID of the user who owns this template
     |      title       (in) - short identifier/title for the template
     |      content     (in) - the full prompt body (may include placeholders)
     |      category    (in) - grouping label (required per schema)
     |      isPrivate   (in) - true if template is private to the user
     |      workspaceID (in) - FK to Workspace if shared, or -1 if private
     |
     | Returns:  the generated templateID, or -1 on failure
     *-----------------------------------------------------------------------*/
    public static int addTemplate(Connection conn, int creatorID, String title,
                                  String content, String category,
                                  boolean isPrivate, int workspaceID) throws SQLException {
        if (title == null || title.trim().isEmpty()) {
            System.out.println("Template title must be non-empty.");
            return -1;
        }
        if (content == null || content.trim().isEmpty()) {
            System.out.println("Template content must be non-empty.");
            return -1;
        }
        if (category == null || category.trim().isEmpty()) {
            System.out.println("Template category must be non-empty.");
            return -1;
        }

        String sql = "INSERT INTO PromptTemplate "                          // parameterized INSERT
                   + "(templateID, creatorID, workspaceID, title, content, "
                   + "category, privateStatus, creationDate) "
                   + "VALUES (SEQ_PROMPTTEMPLATE.NEXTVAL, ?, ?, ?, ?, ?, ?, SYSDATE)";

        PreparedStatement pstmt = conn.prepareStatement(sql, new String[]{"templateID"});
        pstmt.setInt(1, creatorID);

        if (workspaceID < 0) {
            pstmt.setNull(2, Types.INTEGER); // no workspace for private templates
        } else {
            pstmt.setInt(2, workspaceID);    // FK -> Workspace
        }

        pstmt.setString(3, title.trim());
        pstmt.setString(4, content.trim());
        pstmt.setString(5, category.trim());

        if (isPrivate) pstmt.setInt(6, 1); // store private as 1
        else           pstmt.setInt(6, 0); // store shared as 0

        pstmt.executeUpdate();

        ResultSet rs = pstmt.getGeneratedKeys(); // holds the auto-generated templateID
        if (rs.next()) {
            int newID = rs.getInt(1); // the newly created template's PK
            System.out.println("Template added with ID: " + newID);
            pstmt.close();
            return newID;
        }

        pstmt.close();
        return -1;
    }

    /*-------------------------------------------------------------------------
     | Method: updateTemplate
     |
     | Purpose: Updates an existing prompt template's title, content, and/or
     |          category. Only fields with non-null values are changed; passing
     |          null for a field leaves it unchanged in the database.
     |
     | Pre-condition:  A valid, open database connection is provided. templateID
     |                 must reference an existing PromptTemplate row. At least
     |                 one of newTitle, newContent, or newCategory should be
     |                 non-null to make a meaningful update.
     |
     | Post-condition: The matching PromptTemplate row is updated in the database.
     |
     | Parameters:
     |      conn        (in) - open Oracle database connection
     |      templateID  (in) - ID of the template to update
     |      newTitle    (in) - replacement title, or null to leave unchanged
     |      newContent  (in) - replacement content, or null to leave unchanged
     |      newCategory (in) - replacement category, or null to leave unchanged
     |
     | Returns:  true if the template was updated successfully, false otherwise
     *-----------------------------------------------------------------------*/
    public static boolean updateTemplate(Connection conn, int templateID,
                                         String newTitle, String newContent,
                                         String newCategory) throws SQLException {
        StringBuilder setClauses = new StringBuilder(); // accumulates SET assignments dynamically
        int paramCount = 0;                             // tracks how many fields will be updated

        if (newTitle != null && !newTitle.trim().isEmpty()) {
            setClauses.append("title = ?, ");
            paramCount++;
        }
        if (newContent != null && !newContent.trim().isEmpty()) {
            setClauses.append("content = ?, ");
            paramCount++;
        }
        if (newCategory != null && !newCategory.trim().isEmpty()) {
            setClauses.append("category = ?, ");
            paramCount++;
        }

        if (paramCount == 0) {
            System.out.println("No valid fields provided for update.");
            return false;
        }

        String setClause = setClauses.substring(0, setClauses.length() - 2); // strip trailing ", "
        String sql = "UPDATE PromptTemplate SET " + setClause                 // final UPDATE statement
                   + " WHERE templateID = ?";

        PreparedStatement pstmt = conn.prepareStatement(sql);
        int idx = 1; // current parameter index for binding

        if (newTitle != null && !newTitle.trim().isEmpty()) {
            pstmt.setString(idx++, newTitle.trim());
        }
        if (newContent != null && !newContent.trim().isEmpty()) {
            pstmt.setString(idx++, newContent.trim());
        }
        if (newCategory != null && !newCategory.trim().isEmpty()) {
            pstmt.setString(idx++, newCategory.trim());
        }

        pstmt.setInt(idx, templateID);

        int rows = pstmt.executeUpdate(); // number of rows updated
        pstmt.close();

        if (rows == 1) {
            System.out.println("Template " + templateID + " updated successfully.");
            return true;
        }

        System.out.println("No template found with ID: " + templateID);
        return false;
    }
}
