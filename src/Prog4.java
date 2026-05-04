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
 |   Compile/Run:  export CLASSPATH=/usr/lib/oracle/19.8/client64/lib/ojdbc8.jar:${CLASSPATH}
 |                 Compile: javac Prog4.java
 |                 Run:     java Prog4 [username] [password]
 |
 +-----------------------------------------------------------------------------
 |
 |  Description:  This program implements the front-end JDBC client for an
 |                LLM user-facing ecosystem management system. It connects to
 |                an Oracle database hosted on aloe.cs.arizona.edu and allows
 |                users to manage accounts, conversations, workspaces, personas,
 |                prompt templates, subscriptions, billing, and support tickets
 |                through a text-based menu interface. The program also supports
 |                four pre-defined analytical queries over the database.
 |
 |                The application uses a two-tier client-server architecture:
 |                Oracle handles all persistent storage and enforces relational
 |                constraints, while this JDBC front-end handles user interaction
 |                and delegates operations to dedicated manager classes.
 |
 |        Input:  Oracle credentials are read either from the command-line
 |                arguments (preferred) or prompted interactively. All further
 |                input is read from stdin via Scanner during the menu loop.
 |
 |       Output:  Confirmation messages, query results, and error descriptions
 |                are printed to stdout.
 |
 |   Techniques:  JDBC PreparedStatements for all DML; Oracle sequences for
 |                primary key generation; Oracle MERGE for feedback upserts.
 |                Manager classes separate concerns by functional area.
 |
 |   Required Features Not Included:  All required features are included.
 |
 |   Known Bugs:  None.
 |
 *---------------------------------------------------------------------------*/

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Scanner;

public class Prog4 {

    /*-------------------------------------------------------------------------
     | Method: main
     |
     | Purpose: Entry point of the application. Reads Oracle credentials from
     |          command-line arguments if provided, or prompts the user for
     |          them interactively. Opens a JDBC connection, then enters the
     |          main menu loop, delegating each user choice to the appropriate
     |          manager class. Entering '0' exits the loop, commits any
     |          pending work, and closes the connection cleanly.
     |
     | Pre-condition:  The Oracle JDBC driver jar is on the classpath and the
     |                 aloe.cs.arizona.edu server is reachable.
     |
     | Post-condition: All DB operations selected by the user have been
     |                 committed. The connection is closed before exit.
     |
     | Parameters:
     |      args (in) - optional command-line arguments; args[0] is the Oracle
     |                  username and args[1] is the Oracle password
     |
     | Returns: Nothing
     *-----------------------------------------------------------------------*/
    public static void main(String[] args) throws SQLException {
        Scanner scanner = new Scanner(System.in); // reads all keyboard input

        String username; // Oracle login username
        String password; // Oracle login password

        // Prefer credentials passed as arguments; fall back to interactive prompt
        if (args.length == 2) {
            username = args[0];
            password = args[1];
        } else {
            System.out.print("Oracle username: ");
            username = scanner.nextLine().trim();
            System.out.print("Oracle password: ");
            password = scanner.nextLine().trim();
        }

        Connection dbConnection = DBConnection.initConnection(username, password); // active DB connection
        dbConnection.setAutoCommit(false); // manager classes commit their own transactions

        System.out.println("\nWelcome to your personalized AI Environment.");

        boolean stillGoing = true; // controls the main menu loop

        while (stillGoing) {
            showMenu();
            System.out.print("Choice: ");
            String choice = scanner.nextLine().trim(); // user's menu selection this iteration

            switch (choice) {
                case "1":  UserManager.menu(dbConnection, scanner);         break;
                case "2":  ConversationManager.menu(dbConnection, scanner); break;
                case "3":  WorkspaceManager.menu(dbConnection, scanner);    break;
                case "4":  PersonaManager.menu(dbConnection, scanner);      break;
                case "5":  PromptManager.menu(dbConnection, scanner);       break;
                case "6":  UserManager.subscriptionMenu(dbConnection, scanner); break;
                case "7":  UserManager.billingMenu(dbConnection, scanner);  break;
                case "8":  TicketManager.menu(dbConnection, scanner);       break;
                case "9":  QueryManager.menu(dbConnection, scanner);        break;
                case "0":  stillGoing = false;                              break;
                default:   System.out.println("Invalid choice. Please enter 0-9.");
            }
        }

        // Commit any remaining work and release the DB connection
        dbConnection.commit();
        dbConnection.close();
        scanner.close();
        System.out.println("Goodbye.");
    }

    /*-------------------------------------------------------------------------
     | Method: showMenu
     |
     | Purpose: Prints the top-level menu options to stdout. Called once at
     |          the start of each iteration of the main menu loop so the user
     |          always sees the current options before entering a choice.
     |
     | Pre-condition:  None.
     |
     | Post-condition: The menu is printed to stdout. No state is changed.
     |
     | Parameters: None
     |
     | Returns: Nothing
     *-----------------------------------------------------------------------*/
    private static void showMenu() {
        System.out.println("\n========== LLM Menu ==========");
        System.out.println("1. User accounts");
        System.out.println("2. Conversations & messages");
        System.out.println("3. Workspaces");
        System.out.println("4. Personas");
        System.out.println("5. Prompt library");
        System.out.println("6. Subscription tracking");
        System.out.println("7. Billing & invoices");
        System.out.println("8. Support tickets");
        System.out.println("9. Run a query");
        System.out.println("0. Quit");
        System.out.println("===================================");
    }
}
