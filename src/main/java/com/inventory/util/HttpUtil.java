package com.inventory.util;

import com.sun.net.httpserver.HttpExchange;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * HttpUtil — helper methods shared across all handlers.
 */
public class HttpUtil {

    /** Read the full request body as a String. */
    public static String readBody(HttpExchange ex) throws IOException {
        InputStream is = ex.getRequestBody();
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    /** Send a JSON response with the given HTTP status code. */
    public static void sendJson(HttpExchange ex, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        // CORS headers so the browser HTML pages can talk to this server freely
        ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    /** Convenience: send a simple {message: "..."} JSON response. */
    public static void sendMessage(HttpExchange ex, int status, String message) throws IOException {
        JSONObject obj = new JSONObject();
        obj.put("message", message);
        sendJson(ex, status, obj.toString());
    }

    /** Convenience: send a simple {error: "..."} JSON response. */
    public static void sendError(HttpExchange ex, int status, String error) throws IOException {
        JSONObject obj = new JSONObject();
        obj.put("error", error);
        sendJson(ex, status, obj.toString());
    }

    /** Handle CORS pre-flight OPTIONS requests — returns true if it was an OPTIONS call. */
    public static boolean handleOptions(HttpExchange ex) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            ex.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            ex.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            ex.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
            ex.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }

    /** Parse query string parameter from the request URI. e.g. ?id=5 → "5" */
    public static String getQueryParam(HttpExchange ex, String key) {
        String query = ex.getRequestURI().getQuery();
        if (query == null) return null;
        for (String part : query.split("&")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2 && kv[0].equals(key)) return kv[1];
        }
        return null;
    }
}
