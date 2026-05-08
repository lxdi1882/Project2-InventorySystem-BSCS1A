package com.inventory.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DatabaseConnection — single point of truth for the JDBC connection.
 * Update DB_NAME, USER, and PASS to match your XAMPP setup.
 */
public class DatabaseConnection {

    private static final String URL      = "jdbc:mysql://localhost:3306/inventory_db";
    private static final String USER     = "root";   // default XAMPP user
    private static final String PASSWORD = "";       // default XAMPP password (blank)

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
