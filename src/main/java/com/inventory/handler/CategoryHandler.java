package com.inventory.handler;

import com.inventory.db.DatabaseConnection;
import com.inventory.util.HttpUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.*;


 //CategoryHandler — full CRUD for /api/categories
 
 //GET    /api/categories          → list all categories
 //POST   /api/categories          → create a new category
 //PUT    /api/categories?id=N     → update category N
 //DELETE /api/categories?id=N     → delete category N (warns if products exist)
 
public class CategoryHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (HttpUtil.handleOptions(ex)) return;

        String method = ex.getRequestMethod().toUpperCase();
        switch (method) {
            case "GET"    -> getAll(ex);
            case "POST"   -> create(ex);
            case "PUT"    -> update(ex);
            case "DELETE" -> delete(ex);
            default       -> HttpUtil.sendError(ex, 405, "Method not allowed.");
        }
    }

    //GET all categories
    private void getAll(HttpExchange ex) throws IOException {
        String sql = "SELECT category_id, category_name, description FROM categories ORDER BY category_name";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            JSONArray arr = new JSONArray();
            while (rs.next()) {
                JSONObject cat = new JSONObject();
                cat.put("category_id",   rs.getInt("category_id"));
                cat.put("category_name", rs.getString("category_name"));
                cat.put("description",   rs.getString("description") != null ? rs.getString("description") : "");
                arr.put(cat);
            }
            HttpUtil.sendJson(ex, 200, arr.toString());

        } catch (SQLException e) {
            e.printStackTrace();
            HttpUtil.sendError(ex, 500, "Database error: " + e.getMessage());
        }
    }

    //POST create category
    private void create(HttpExchange ex) throws IOException {
        try {
            JSONObject req = new JSONObject(HttpUtil.readBody(ex));
            String name = req.optString("category_name", "").trim();
            String desc = req.optString("description", "").trim();

            if (name.isEmpty()) {
                HttpUtil.sendError(ex, 400, "Category name is required.");
                return;
            }

            String sql = "INSERT INTO categories (category_name, description) VALUES (?, ?)";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                stmt.setString(1, name);
                stmt.setString(2, desc.isEmpty() ? null : desc);
                stmt.executeUpdate();

                ResultSet keys = stmt.getGeneratedKeys();
                int newId = keys.next() ? keys.getInt(1) : -1;

                JSONObject resp = new JSONObject();
                resp.put("category_id", newId);
                resp.put("message", "Category created.");
                HttpUtil.sendJson(ex, 201, resp.toString());
            }

        } catch (SQLIntegrityConstraintViolationException e) {
            HttpUtil.sendError(ex, 409, "Category name already exists.");
        } catch (Exception e) {
            e.printStackTrace();
            HttpUtil.sendError(ex, 500, "Server error: " + e.getMessage());
        }
    }

    //PUT update category
    private void update(HttpExchange ex) throws IOException {
        String idStr = HttpUtil.getQueryParam(ex, "id");
        if (idStr == null) { HttpUtil.sendError(ex, 400, "Missing ?id= parameter."); return; }

        try {
            int id = Integer.parseInt(idStr);
            JSONObject req = new JSONObject(HttpUtil.readBody(ex));
            String name = req.optString("category_name", "").trim();
            String desc = req.optString("description", "").trim();

            if (name.isEmpty()) { HttpUtil.sendError(ex, 400, "Category name is required."); return; }

            String sql = "UPDATE categories SET category_name = ?, description = ? WHERE category_id = ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, name);
                stmt.setString(2, desc.isEmpty() ? null : desc);
                stmt.setInt(3, id);
                int rows = stmt.executeUpdate();

                if (rows == 0) HttpUtil.sendError(ex, 404, "Category not found.");
                else           HttpUtil.sendMessage(ex, 200, "Category updated.");
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            HttpUtil.sendError(ex, 409, "Category name already exists.");
        } catch (Exception e) {
            e.printStackTrace();
            HttpUtil.sendError(ex, 500, "Server error: " + e.getMessage());
        }
    }

    //DELETE category
    private void delete(HttpExchange ex) throws IOException {
        String idStr = HttpUtil.getQueryParam(ex, "id");
        if (idStr == null) { HttpUtil.sendError(ex, 400, "Missing ?id= parameter."); return; }

        try {
            int id = Integer.parseInt(idStr);

            // Check if any products reference this category
            String checkSql = "SELECT COUNT(*) FROM products WHERE category_id = ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement check = conn.prepareStatement(checkSql)) {

                check.setInt(1, id);
                ResultSet rs = check.executeQuery();
                rs.next();
                int count = rs.getInt(1);

                if (count > 0) {
                    HttpUtil.sendError(ex, 409, "Cannot delete: " + count + " product(s) still assigned to this category.");
                    return;
                }

                String sql = "DELETE FROM categories WHERE category_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setInt(1, id);
                    int rows = stmt.executeUpdate();
                    if (rows == 0) HttpUtil.sendError(ex, 404, "Category not found.");
                    else           HttpUtil.sendMessage(ex, 200, "Category deleted.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            HttpUtil.sendError(ex, 500, "Server error: " + e.getMessage());
        }
    }
}
