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

        Connection dbConnection = DBConnection.initConnection(username, password);
        System.out.println("Welcome to your personalized AI Environment\n"
                + "What would you like to do?\n");
        while (stillGoing) {
            System.out.println("Ask Some Queries\n");
            System.out.println("Handle my Environment");
        }
    }
}