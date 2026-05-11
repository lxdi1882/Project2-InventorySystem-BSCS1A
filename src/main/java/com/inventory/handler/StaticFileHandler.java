package com.inventory.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;


  //StaticFileHandler — serves the HTML/CSS/JS frontend files from the
  //resources/static directory bundled inside the JAR.
 
public class StaticFileHandler implements HttpHandler {

    private static final Map<String, String> MIME = new HashMap<>();
    static {
        MIME.put("html", "text/html; charset=UTF-8");
        MIME.put("css",  "text/css");
        MIME.put("js",   "application/javascript");
        MIME.put("ico",  "image/x-icon");
        MIME.put("png",  "image/png");
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        URI uri = ex.getRequestURI();
        String path = uri.getPath();

        //Default route → login page
        if (path.equals("/") || path.isEmpty()) {
            path = "/pages/login.html";
        }

        //Build the resource path inside the JAR
        String resourcePath = "/static" + path;

        InputStream stream = getClass().getResourceAsStream(resourcePath);

        if (stream == null) {
            String notFound = "404 - Not Found: " + path;
            byte[] bytes = notFound.getBytes();
            ex.sendResponseHeaders(404, bytes.length);
            try (OutputStream os = ex.getResponseBody()) { os.write(bytes); }
            return;
        }

        // Determine MIME type from extension
        String ext = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : "html";
        String mime = MIME.getOrDefault(ext, "application/octet-stream");

        byte[] bytes = stream.readAllBytes();
        ex.getResponseHeaders().set("Content-Type", mime);
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }
}
