package com.inventory.handler;

import com.inventory.db.DatabaseConnection;
import com.inventory.util.HttpUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.*;

/**
 * ProductHandler — full CRUD for /api/products
 *
 * GET    /api/products            → list all products (with JOIN to categories)
 * GET    /api/products?low=true   → list only Low Stock / Out of Stock products
 * POST   /api/products            → create a new product
 * PUT    /api/products?id=N       → update product N
 * DELETE /api/products?id=N       → delete product N
 */
public class ProductHandler implements HttpHandler {

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

    // ── Derive stock status string ───────────────────────────────────────────
    private String getStatus(int qty, int threshold) {
        if (qty == 0)              return "Out of Stock";
        if (qty <= threshold)      return "Low Stock";
        return "In Stock";
    }

    // ── GET products ─────────────────────────────────────────────────────────
    private void getAll(HttpExchange ex) throws IOException {
        String lowOnly = HttpUtil.getQueryParam(ex, "low");
        boolean filterLow = "true".equalsIgnoreCase(lowOnly);

        String sql = """
                SELECT p.product_id, p.product_name, p.unit, p.unit_price,
                       p.stock_quantity, p.low_stock_threshold, p.created_at,
                       c.category_id, c.category_name
                FROM products p
                JOIN categories c ON p.category_id = c.category_id
                ORDER BY p.product_name
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            JSONArray arr = new JSONArray();
            while (rs.next()) {
                int qty       = rs.getInt("stock_quantity");
                int threshold = rs.getInt("low_stock_threshold");
                String status = getStatus(qty, threshold);

                if (filterLow && status.equals("In Stock")) continue;

                JSONObject p = new JSONObject();
                p.put("product_id",         rs.getInt("product_id"));
                p.put("product_name",       rs.getString("product_name"));
                p.put("category_id",        rs.getInt("category_id"));
                p.put("category_name",      rs.getString("category_name"));
                p.put("unit",               rs.getString("unit"));
                p.put("unit_price",         rs.getDouble("unit_price"));
                p.put("stock_quantity",     qty);
                p.put("low_stock_threshold",threshold);
                p.put("status",             status);
                p.put("created_at",         rs.getString("created_at"));
                arr.put(p);
            }
            HttpUtil.sendJson(ex, 200, arr.toString());

        } catch (SQLException e) {
            e.printStackTrace();
            HttpUtil.sendError(ex, 500, "Database error: " + e.getMessage());
        }
    }

    // ── POST create product ──────────────────────────────────────────────────
    private void create(HttpExchange ex) throws IOException {
        try {
            JSONObject req = new JSONObject(HttpUtil.readBody(ex));

            String name      = req.optString("product_name", "").trim();
            int    catId     = req.optInt("category_id", 0);
            String unit      = req.optString("unit", "").trim();
            double price     = req.optDouble("unit_price", -1);
            int    qty       = req.optInt("stock_quantity", -1);
            int    threshold = req.optInt("low_stock_threshold", -1);

            // Validation
            if (name.isEmpty() || catId == 0 || unit.isEmpty())
                { HttpUtil.sendError(ex, 400, "Product name, category, and unit are required."); return; }
            if (price <= 0)
                { HttpUtil.sendError(ex, 400, "Unit price must be a positive number."); return; }
            if (qty < 0)
                { HttpUtil.sendError(ex, 400, "Stock quantity must be zero or more."); return; }
            if (threshold <= 0)
                { HttpUtil.sendError(ex, 400, "Low stock threshold must be a positive number."); return; }

            String sql = """
                    INSERT INTO products (product_name, category_id, unit, unit_price, stock_quantity, low_stock_threshold)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """;

            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                stmt.setString(1, name);
                stmt.setInt(2, catId);
                stmt.setString(3, unit);
                stmt.setDouble(4, price);
                stmt.setInt(5, qty);
                stmt.setInt(6, threshold);
                stmt.executeUpdate();

                ResultSet keys = stmt.getGeneratedKeys();
                int newId = keys.next() ? keys.getInt(1) : -1;

                JSONObject resp = new JSONObject();
                resp.put("product_id", newId);
                resp.put("message", "Product created.");
                HttpUtil.sendJson(ex, 201, resp.toString());
            }

        } catch (SQLIntegrityConstraintViolationException e) {
            HttpUtil.sendError(ex, 409, "A product with this name already exists in this category.");
        } catch (Exception e) {
            e.printStackTrace();
            HttpUtil.sendError(ex, 500, "Server error: " + e.getMessage());
        }
    }

    // ── PUT update product ───────────────────────────────────────────────────
    private void update(HttpExchange ex) throws IOException {
        String idStr = HttpUtil.getQueryParam(ex, "id");
        if (idStr == null) { HttpUtil.sendError(ex, 400, "Missing ?id= parameter."); return; }

        try {
            int id = Integer.parseInt(idStr);
            JSONObject req = new JSONObject(HttpUtil.readBody(ex));

            String name      = req.optString("product_name", "").trim();
            int    catId     = req.optInt("category_id", 0);
            String unit      = req.optString("unit", "").trim();
            double price     = req.optDouble("unit_price", -1);
            int    threshold = req.optInt("low_stock_threshold", -1);

            if (name.isEmpty() || catId == 0 || unit.isEmpty())
                { HttpUtil.sendError(ex, 400, "All fields are required."); return; }
            if (price <= 0)
                { HttpUtil.sendError(ex, 400, "Unit price must be a positive number."); return; }
            if (threshold <= 0)
                { HttpUtil.sendError(ex, 400, "Low stock threshold must be a positive number."); return; }

            String sql = """
                    UPDATE products
                    SET product_name = ?, category_id = ?, unit = ?, unit_price = ?, low_stock_threshold = ?
                    WHERE product_id = ?
                    """;

            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, name);
                stmt.setInt(2, catId);
                stmt.setString(3, unit);
                stmt.setDouble(4, price);
                stmt.setInt(5, threshold);
                stmt.setInt(6, id);

                int rows = stmt.executeUpdate();
                if (rows == 0) HttpUtil.sendError(ex, 404, "Product not found.");
                else           HttpUtil.sendMessage(ex, 200, "Product updated.");
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            HttpUtil.sendError(ex, 409, "A product with this name already exists in this category.");
        } catch (Exception e) {
            e.printStackTrace();
            HttpUtil.sendError(ex, 500, "Server error: " + e.getMessage());
        }
    }

    // ── DELETE product ───────────────────────────────────────────────────────
    private void delete(HttpExchange ex) throws IOException {
        String idStr = HttpUtil.getQueryParam(ex, "id");
        if (idStr == null) { HttpUtil.sendError(ex, 400, "Missing ?id= parameter."); return; }

        try {
            int id = Integer.parseInt(idStr);
            String sql = "DELETE FROM products WHERE product_id = ?";

            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, id);
                int rows = stmt.executeUpdate();
                if (rows == 0) HttpUtil.sendError(ex, 404, "Product not found.");
                else           HttpUtil.sendMessage(ex, 200, "Product deleted.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            HttpUtil.sendError(ex, 500, "Server error: " + e.getMessage());
        }
    }
}
