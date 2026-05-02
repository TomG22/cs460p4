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
 |  Description:  This program prompts the user to choose from 4 queries. This
 |                program requires being able to access the created and filled
 |                tables from TableMaker.java and TableFiller.java.
 |
 |
 |        Input: fileName.csv - a csv file that (if non-empty) contains data in
 |               the 11 data fields from the given bat cave dataset
 |
 |       Output: fileName.bin - a binary file containing the data from the csv
 |               file.
 |
 |   Techniques:
 |
 |   Required Features Not Included:  All required features are included.
 |
 |   Known Bugs: None
 |
 *---------------------------------------------------------------------------*/

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Scanner;

public class Prog4 {
    /*-------------------------------------------------------------------------
     | Method: initConnection (String username, String password)
     |
     | Purpose: Establishes a connection to Oracle. Begins by loading in the
     |          JDBC driver, then creates the connection and returns it to the
     |          user.
     |
     |
     | Pre-condition: The Oracle JDBC driver should be added to the classpath
     |                environment variable. The provided username and password
     |                should be a correct Oracle username/password match.
     |
     | Post-condition: The connection to the DBMS in Oracle is established.
     |
     |
     | Parameters:
     |      username - the provided oracle username
     |      password - the provided oracle password
     |
     | Returns:
     |      Connection - the initialized and connected Oracle connection object
     *-----------------------------------------------------------------------*/
    private Connection initConnection(String username, String password) {
        String oracleURL = "jdbc:oracle:thin:@aloe.cs.arizona.edu:1521:oracle";

        // Attempting to connect to oracle driver
        try {
            Class.forName("oracle.jdbc.OracleDriver");
        } catch (ClassNotFoundException e) {
            System.out.println("Issue loading JDBC driver\n"
                    + "Try checking the classpath");
            System.exit(-1);
        }

        // Attempting to set up connection to oracle server
        try {
            return DriverManager.getConnection(oracleURL, username, password);
        } catch (SQLException e) {
            System.out.println("Issue connecting to oracle\n");
            System.exit(-1);
        }
        return null; // Should not reach here, TERRIBLE error if so
    }

    public static void main(String[] args) {
        Prog4 program = new Prog4();
        String username, password;
        Scanner scanner = new Scanner(System.in);
        boolean stillGoing = true;

        // Checking for missing argument amount
        if (args.length == 2) {
            username = args[0];
            password = args[1];
        } else {
            System.out.println("Please enter your username: ");
            username = scanner.nextLine();
            System.out.println("Please enter your password: ");
            password = scanner.nextLine();
        }

        Connection dbConnection = program.initConnection(username, password);
        System.out.println("Welcome to your personalized AI Environment\n"
                + "What would you like to do?\n");
        while (stillGoing) {
            System.out.println("Ask Some Queries");
            System.out.println("Handle my Environment");
        }
    }
}