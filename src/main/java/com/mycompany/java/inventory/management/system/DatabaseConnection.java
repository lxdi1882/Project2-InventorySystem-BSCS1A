/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.mycompany.java.inventory.management.system;

/**
 *
 * @author lxdi1
 */
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class DatabaseConnection {

    // Connection settings
    static final String DB_URL  = "jdbc:mysql://localhost:3306/db_company";
    static final String USER    = "root";
    static final String PASS    = "";  // XAMPP default is empty

    public static void main(String[] args) {
        Connection conn = null;
        Statement  stmt = null;

        try {
            // 1. Load the JDBC driver (optional for newer versions)
            Class.forName("com.mysql.cj.jdbc.Driver");

            // 2. Open a connection
            System.out.println("Connecting to database...");
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            System.out.println("Connected successfully!");

            // 3. Execute a query
            stmt = conn.createStatement();
            String sql = "SELECT * FROM tbl_employee";
            ResultSet rs = stmt.executeQuery(sql);

            // 4. Process the result set
            while (rs.next()) {
                // Replace 'id' and 'name' with your actual column names
                 int id          = rs.getInt("employee_id");
    String name     = rs.getString("employee_name");
    String position = rs.getString("employee_position");
    double salary   = rs.getDouble("employee_salary");
    int deptId      = rs.getInt("dept_id");

                
                System.out.println("ID: " + id + 
                       " | Name: " + name + 
                       " | Position: " + position + 
                       " | Salary: " + salary + 
                       " | Dept: " + deptId);
            }

            // 5. Clean up
            rs.close();
            stmt.close();
            conn.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}