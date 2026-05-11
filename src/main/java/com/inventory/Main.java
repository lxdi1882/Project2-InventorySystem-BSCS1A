package com.inventory;

import com.inventory.handler.*;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;



 //Starts an HTTP server on port 8080, registers all API routes,
 // and serves the static HTML/CSS/JS frontend.
 
 //open:  http://localhost:8080
 
public class Main {

    private static final int PORT = 8080;

    public static void main(String[] args) throws Exception {

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
          //API routes
        server.createContext("/api/auth",      new AuthHandler());
        server.createContext("/api/categories",new CategoryHandler());
        //server.createContext("/api/products",  new ProductHandler());
        //server.createContext("/api/stock",     new StockHandler());
        //server.createContext("/api/dashboard", new DashboardHandler());

        //Static frontend (HTML/CSS/JS)
       server.createContext("/", new StaticFileHandler());

        //thread pool
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();

        System.out.println("---------------------------------------------------------");
        System.out.println("  Inventory System running at http://localhost:" + PORT);
        System.out.println("  Open your browser and go to that address.");
        System.out.println("  Press Ctrl+C to stop the server.");
        System.out.println("---------------------------------------------------------");
        
        
    }
}
