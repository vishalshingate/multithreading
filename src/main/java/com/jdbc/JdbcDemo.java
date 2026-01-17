package com.jdbc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class JdbcDemo {

    // JDBC URL for H2 In-Memory Database
    private static final String JDBC_URL = "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1";
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    public static void main(String[] args) {
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            // Optional: Load Driver manually (for older JDBC versions or explicit loading)
            Class.forName("org.h2.Driver");

            // 1. Establish the Connection
            System.out.println("Connecting to database...");
            conn = DriverManager.getConnection(JDBC_URL, USER, PASSWORD);

            // 2. Create a Table
            System.out.println("Creating table...");
            stmt = conn.createStatement();
            String sql = "CREATE TABLE IF NOT EXISTS USERS " +
                         "(id INTEGER not NULL, " +
                         " first_name VARCHAR(255), " +
                         " last_name VARCHAR(255), " +
                         " PRIMARY KEY ( id ))";
            stmt.executeUpdate(sql);

            // 3. Insert Data (Using PreparedStatement for security/performance)
            System.out.println("Inserting data...");
            String insertSql = "INSERT INTO USERS (id, first_name, last_name) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                pstmt.setInt(1, 100);
                pstmt.setString(2, "Zara");
                pstmt.setString(3, "Ali");
                pstmt.executeUpdate();

                pstmt.setInt(1, 101);
                pstmt.setString(2, "Mahnaz");
                pstmt.setString(3, "Fatma");
                pstmt.executeUpdate();
            }

            // 4. Query Data
            System.out.println("Reading data...");
            String selectSql = "SELECT id, first_name, last_name FROM USERS";
            rs = stmt.executeQuery(selectSql);

            while (rs.next()) {
                int id = rs.getInt("id");
                String first = rs.getString("first_name");
                String last = rs.getString("last_name");

                System.out.print("ID: " + id);
                System.out.print(", First: " + first);
                System.out.println(", Last: " + last);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            // Handle error for Class.forName
            System.err.println("JDBC Driver class not found. Include the JDBC driver in your library path.");
            e.printStackTrace();
        } finally {
            // 5. Close Resources (Manually in 'finally' block or use Try-With-Resources)
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
                System.out.println("Resources closed.");
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
    }
}
