import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
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
    public static Connection initConnection(String username, String password) {
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
}
