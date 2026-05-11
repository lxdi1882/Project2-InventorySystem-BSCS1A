package com.inventory.handler;

import com.inventory.db.DatabaseConnection;
import com.inventory.util.HttpUtil;
import com.inventory.util.PasswordUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;


 //AuthHandler — handles POST /api/auth/login and POST /api/auth/logout
 //Updated to match actual DB schema: first_name + middle_name + last_name
 
public class AuthHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (HttpUtil.handleOptions(ex)) return;

        String path = ex.getRequestURI().getPath();

        if (path.endsWith("/login") && "POST".equalsIgnoreCase(ex.getRequestMethod())) {
            handleLogin(ex);
        } else if (path.endsWith("/logout") && "POST".equalsIgnoreCase(ex.getRequestMethod())) {
            HttpUtil.sendMessage(ex, 200, "Logged out successfully.");
        } else {
            HttpUtil.sendError(ex, 404, "Auth endpoint not found.");
        }
    }

    private void handleLogin(HttpExchange ex) throws IOException {
        try {
            String body = HttpUtil.readBody(ex);
            JSONObject req = new JSONObject(body);
            String username = req.optString("username", "").trim();
            String password = req.optString("password", "").trim();

            if (username.isEmpty() || password.isEmpty()) {
                HttpUtil.sendError(ex, 400, "Username and password are required.");
                return;
            }

            String sql = "SELECT user_id, username, password, first_name, middle_name, last_name FROM users WHERE username = ?";

            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, username);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    String storedHash = rs.getString("password");

                    if (PasswordUtil.verify(password, storedHash)) {
                        // Build full name from parts
                        String firstName  = rs.getString("first_name");
                        String middleName = rs.getString("middle_name");
                        String lastName   = rs.getString("last_name");
                        String fullName   = firstName
                                + (middleName != null && !middleName.isBlank() ? " " + middleName : "")
                                + (lastName  != null && !lastName.isBlank()   ? " " + lastName   : "");

                        JSONObject resp = new JSONObject();
                        resp.put("user_id",   rs.getInt("user_id"));
                        resp.put("username",  rs.getString("username"));
                        resp.put("full_name", fullName.trim());
                        resp.put("message",   "Login successful.");
                        HttpUtil.sendJson(ex, 200, resp.toString());
                    } else {
                        HttpUtil.sendError(ex, 401, "Incorreect password.");
                        System.out.println("Typed password: " + password);
System.out.println("Hashed typed: " + PasswordUtil.hash(password));
System.out.println("DB password: " + storedHash);
                    }
                } else {
                    HttpUtil.sendError(ex, 401, "User not found.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            HttpUtil.sendError(ex, 500, "Server error: " + e.getMessage());
        }
    }
}