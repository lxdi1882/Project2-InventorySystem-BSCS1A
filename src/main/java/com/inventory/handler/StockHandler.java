package com.inventory.handler;

import com.inventory.db.DatabaseConnection;
import com.inventory.util.HttpUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.*;


 //StockHandler — handles /api/stock/restock and /api/stock/deduct
 
 //POST /api/stock/restock   { product_id, quantity }  → adds stock
 //POST /api/stock/deduct    { product_id, quantity }  → deducts stock (validated)
 
public class StockHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (HttpUtil.handleOptions(ex)) return;

        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            HttpUtil.sendError(ex, 405, "Method not allowed.");
            return;
        }

        String path = ex.getRequestURI().getPath();

        if (path.endsWith("/restock")) {
            handleRestock(ex);
        } else if (path.endsWith("/deduct")) {
            handleDeduct(ex);
        } else {
            HttpUtil.sendError(ex, 404, "Stock endpoint not found.");
        }
    }

    // ── Restock ───────────────────────────────────────────────────────────────
    private void handleRestock(HttpExchange ex) throws IOException {
        try {
            JSONObject req = new JSONObject(HttpUtil.readBody(ex));
            int productId = req.optInt("product_id", 0);
            int quantity  = req.optInt("quantity", 0);

            if (productId == 0) { HttpUtil.sendError(ex, 400, "product_id is required."); return; }
            if (quantity <= 0)  { HttpUtil.sendError(ex, 400, "Quantity must be a positive number."); return; }

            String sql = "UPDATE products SET stock_quantity = stock_quantity + ? WHERE product_id = ?";

            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, quantity);
                stmt.setInt(2, productId);
                int rows = stmt.executeUpdate();

                if (rows == 0) HttpUtil.sendError(ex, 404, "Product not found.");
                else           HttpUtil.sendMessage(ex, 200, "Stock restocked successfully.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            HttpUtil.sendError(ex, 500, "Server error: " + e.getMessage());
        }
    }

    // ── Deduct ────────────────────────────────────────────────────────────────
    private void handleDeduct(HttpExchange ex) throws IOException {
        try {
            JSONObject req = new JSONObject(HttpUtil.readBody(ex));
            int productId = req.optInt("product_id", 0);
            int quantity  = req.optInt("quantity", 0);

            if (productId == 0) { HttpUtil.sendError(ex, 400, "product_id is required."); return; }
            if (quantity <= 0)  { HttpUtil.sendError(ex, 400, "Quantity must be a positive number."); return; }

            // Check current stock first
            String checkSql = "SELECT stock_quantity FROM products WHERE product_id = ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement check = conn.prepareStatement(checkSql)) {

                check.setInt(1, productId);
                ResultSet rs = check.executeQuery();

                if (!rs.next()) { HttpUtil.sendError(ex, 404, "Product not found."); return; }

                int currentStock = rs.getInt("stock_quantity");
                if (quantity > currentStock) {
                    HttpUtil.sendError(ex, 400,
                            "Cannot deduct " + quantity + " — only " + currentStock + " in stock.");
                    return;
                }

                String updateSql = "UPDATE products SET stock_quantity = stock_quantity - ? WHERE product_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                    stmt.setInt(1, quantity);
                    stmt.setInt(2, productId);
                    stmt.executeUpdate();
                    HttpUtil.sendMessage(ex, 200, "Stock deducted successfully.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            HttpUtil.sendError(ex, 500, "Server error: " + e.getMessage());
        }
    }
}
