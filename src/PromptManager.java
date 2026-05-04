/*-----------------------------------------------------------------------------
 |
 |  Class Name:  PromptManager
 |
 |  Purpose:  Manages reusable prompt templates for the LLM platform
 |            (Functionality 5). Templates allow users to save and reuse
 |            commonly used prompt structures, optionally grouped by
 |            category. Templates can be created and updated in-place
 |            without replacing the entire record.
 |
 |  Packages:  java.sql
 |             java.util.Scanner
 |
 |  Methods:
 |      menu()           - Displays sub-menu and routes to methods
 |      addTemplate()    - Inserts a new prompt template for a user
 |      updateTemplate() - Updates fields on an existing prompt template
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
        int userId = Integer.parseInt(scanner.nextLine().trim()); // FK -> ApplicationUser

        System.out.print("Template name: ");
        String name = scanner.nextLine().trim();                  // short title for the template

        System.out.print("Template text: ");
        String templateText = scanner.nextLine().trim();          // full prompt body

        System.out.print("Category (press Enter to skip): ");
        String category = scanner.nextLine().trim();              // optional grouping label
        if (category.isEmpty()) {
            category = null; // treat empty input as no category
        }

        System.out.print("Share this template within a workspace? (y/n): ");
        boolean isShared = scanner.nextLine().trim().equalsIgnoreCase("y"); // whether template is shared

        // Only prompt for a workspace ID if the template is being shared
        int workspaceId = -1; // FK -> Workspace, -1 means private (no workspace)
        if (isShared) {
            System.out.print("Workspace ID to share within: ");
            workspaceId = Integer.parseInt(scanner.nextLine().trim());
        }

        int newId = addTemplate(conn, userId, name, templateText, category, isShared, workspaceId); // generated template PK
        if (newId != -1) {
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
        int templateId = Integer.parseInt(scanner.nextLine().trim()); // PK of template to update

        // Entering nothing for a field leaves it unchanged in the database
        System.out.print("New name (press Enter to skip): ");
        String newName = scanner.nextLine().trim();                   // replacement name, or empty

        System.out.print("New template text (press Enter to skip): ");
        String newTemplateText = scanner.nextLine().trim();           // replacement body, or empty

        System.out.print("New category (press Enter to skip): ");
        String newCategory = scanner.nextLine().trim();               // replacement category, or empty

        if (newName.isEmpty())         newName         = null;
        if (newTemplateText.isEmpty()) newTemplateText = null;
        if (newCategory.isEmpty())     newCategory     = null;

        boolean success = updateTemplate(conn, templateId, newName, newTemplateText, newCategory); // result of update
        if (success) {
            conn.commit();
        }
    }

    /*-------------------------------------------------------------------------
     | Method: addTemplate
     |
     | Purpose: Inserts a new reusable prompt template into the database.
     |          Templates let users save commonly used prompt structures and
     |          reference them later by name or category. A template may be
     |          private to the user or shared within a specific workspace.
     |
     | Pre-condition:  A valid, open database connection is provided. userId
     |                 must reference an existing User row. name and
     |                 templateText must be non-null and non-empty. If
     |                 isShared is true, workspaceId must reference an
     |                 existing Workspace row; otherwise pass -1.
     |
     | Post-condition: A new PromptTemplate row is inserted into the database.
     |
     | Parameters:
     |      conn         (in) - open Oracle database connection
     |      userId       (in) - ID of the user who owns this template
     |      name         (in) - short identifier/title for the template
     |      templateText (in) - the full prompt body (may include placeholders)
     |      category     (in) - optional grouping label (may be null)
     |      isShared     (in) - true if the template is shared within a workspace
     |      workspaceId  (in) - FK to Workspace if shared, or -1 if private
     |
     | Returns:  the generated template_id, or -1 on failure
     *-----------------------------------------------------------------------*/
    public static int addTemplate(Connection conn, int userId, String name,
                                  String templateText, String category,
                                  boolean isShared, int workspaceId) throws SQLException {
        if (name == null || name.trim().isEmpty()) {
            System.out.println("Template name must be non-empty.");
            return -1;
        }
        if (templateText == null || templateText.trim().isEmpty()) {
            System.out.println("Template text must be non-empty.");
            return -1;
        }

        // NOTE: assumes PromptTemplate has is_shared (NUMBER(1)) and workspace_id (nullable) columns
        String sql = "INSERT INTO PromptTemplate (user_id, name, "  // parameterized INSERT
                   + "template_text, category, is_shared, workspace_id, created_at) "
                   + "VALUES (?, ?, ?, ?, ?, ?, SYSDATE)";

        PreparedStatement pstmt = conn.prepareStatement(sql, new String[]{"template_id"});
        pstmt.setInt(1, userId);
        pstmt.setString(2, name.trim());
        pstmt.setString(3, templateText.trim());

        if (category == null || category.trim().isEmpty()) {
            pstmt.setNull(4, Types.VARCHAR); // category is optional
        } else {
            pstmt.setString(4, category.trim());
        }

        if (isShared) pstmt.setInt(5, 1); // store shared as 1
        else          pstmt.setInt(5, 0); // store private as 0

        if (workspaceId < 0) {
            pstmt.setNull(6, Types.INTEGER); // no workspace for private templates
        } else {
            pstmt.setInt(6, workspaceId);    // FK -> Workspace
        }

        pstmt.executeUpdate();

        ResultSet rs = pstmt.getGeneratedKeys(); // holds the auto-generated template_id
        if (rs.next()) {
            int newId = rs.getInt(1); // the newly created template's PK
            System.out.println("Template added with ID: " + newId);
            pstmt.close();
            return newId;
        }

        pstmt.close();
        return -1;
    }

    /*-------------------------------------------------------------------------
     | Method: updateTemplate
     |
     | Purpose: Updates an existing prompt template's name, body text, and/or
     |          category. Only fields with non-null values are changed; passing
     |          null for a field leaves it unchanged in the database.
     |
     | Pre-condition:  A valid, open database connection is provided. templateId
     |                 must reference an existing PromptTemplate row. At least
     |                 one of newName, newTemplateText, or newCategory should
     |                 be non-null to make a meaningful update.
     |
     | Post-condition: The matching PromptTemplate row is updated in the database.
     |
     | Parameters:
     |      conn            (in) - open Oracle database connection
     |      templateId      (in) - ID of the template to update
     |      newName         (in) - replacement name, or null to leave unchanged
     |      newTemplateText (in) - replacement body text, or null to leave unchanged
     |      newCategory     (in) - replacement category, or null to leave unchanged
     |
     | Returns:  true if the template was updated successfully, false otherwise
     *-----------------------------------------------------------------------*/
    public static boolean updateTemplate(Connection conn, int templateId,
                                         String newName,
                                         String newTemplateText,
                                         String newCategory) throws SQLException {
        StringBuilder setClauses = new StringBuilder(); // accumulates SET assignments dynamically
        int paramCount = 0;                             // tracks how many fields will be updated

        if (newName != null && !newName.trim().isEmpty()) {
            setClauses.append("name = ?, ");
            paramCount++;
        }
        if (newTemplateText != null && !newTemplateText.trim().isEmpty()) {
            setClauses.append("template_text = ?, ");
            paramCount++;
        }
        if (newCategory != null) {
            setClauses.append("category = ?, ");
            paramCount++;
        }

        if (paramCount == 0) {
            System.out.println("No valid fields provided for update.");
            return false;
        }

        String setClause = setClauses.substring(0, setClauses.length() - 2); // strip trailing ", "
        String sql = "UPDATE PromptTemplate SET " + setClause                 // final UPDATE statement
                   + " WHERE template_id = ?";

        PreparedStatement pstmt = conn.prepareStatement(sql);
        int idx = 1; // current parameter index for binding

        if (newName != null && !newName.trim().isEmpty()) {
            pstmt.setString(idx++, newName.trim());
        }
        if (newTemplateText != null && !newTemplateText.trim().isEmpty()) {
            pstmt.setString(idx++, newTemplateText.trim());
        }
        if (newCategory != null) {
            if (newCategory.trim().isEmpty()) {
                pstmt.setNull(idx++, Types.VARCHAR); // clear the category field
            } else {
                pstmt.setString(idx++, newCategory.trim());
            }
        }

        pstmt.setInt(idx, templateId);

        int rows = pstmt.executeUpdate(); // number of rows updated
        pstmt.close();

        if (rows == 1) {
            System.out.println("Template " + templateId + " updated successfully.");
            return true;
        }

        System.out.println("No template found with ID: " + templateId);
        return false;
    }
}
