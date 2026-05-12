package com.inventory.handler;

import com.inventory.db.DatabaseConnection;
import com.inventory.util.HttpUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.json.JSONObject;

import java.io.IOException;
import java.sql.*;

/**
 * DashboardHandler — GET /api/dashboard
 * Returns live summary stats for the dashboard cards.
 */
public class DashboardHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (HttpUtil.handleOptions(ex)) return;

        if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) {
            HttpUtil.sendError(ex, 405, "Method not allowed.");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {

            // Total distinct products
            int totalProducts = queryInt(conn,
                    "SELECT COUNT(*) FROM products");

            // Total items in stock (sum of all quantities)
            int totalItems = queryInt(conn,
                    "SELECT COALESCE(SUM(stock_quantity), 0) FROM products");

            // Total inventory value: SUM(unit_price * stock_quantity)
            double totalValue = queryDouble(conn,
                    "SELECT COALESCE(SUM(unit_price * stock_quantity), 0) FROM products");

            // Low stock count: quantity <= threshold AND quantity > 0, plus out of stock
            int lowStockCount = queryInt(conn,
                    "SELECT COUNT(*) FROM products WHERE stock_quantity <= low_stock_threshold");

            JSONObject resp = new JSONObject();
            resp.put("total_products",  totalProducts);
            resp.put("total_items",     totalItems);
            resp.put("total_value",     Math.round(totalValue * 100.0) / 100.0);
            resp.put("low_stock_count", lowStockCount);

            HttpUtil.sendJson(ex, 200, resp.toString());

        } catch (Exception e) {
            e.printStackTrace();
            HttpUtil.sendError(ex, 500, "Server error: " + e.getMessage());
        }
    }

    private int queryInt(Connection conn, String sql) throws SQLException {
        try (PreparedStatement st = conn.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private double queryDouble(Connection conn, String sql) throws SQLException {
        try (PreparedStatement st = conn.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {
            return rs.next() ? rs.getDouble(1) : 0.0;
        }
    }
}
