package com.magmaguy.resourcepackmanager.autohost;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import lombok.Getter;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;
import java.util.concurrent.Executors;

public class LocalResourcePackServer {
    private HttpServer server;
    @Getter
    private static LocalResourcePackServer instance;
    private Connection dbConnection;
    
    @Getter
    private boolean running = false;

    public LocalResourcePackServer() {
        instance = this;
    }

    public void start() throws RuntimeException {
        try {
            // Initialize database
            initializeDatabase();
            
            // Create packs directory if it doesn't exist
            Files.createDirectories(LocalServerConfig.getResourcePacksPath());

            // Create and start the HTTP server
            server = HttpServer.create(new InetSocketAddress(LocalServerConfig.getHost(), LocalServerConfig.getPort()), 0);
            server.createContext("/", new ResourcePackHandler());
            server.setExecutor(Executors.newFixedThreadPool(10));
            server.start();
            running = true;

            Bukkit.getLogger().info("Local resource pack server started on http://" + 
                       LocalServerConfig.getHost() + ":" + 
                       LocalServerConfig.getPort() + "/");
        } catch (Exception e) {
            running = false;
            server = null;
            Bukkit.getLogger().severe("Failed to start local resource pack server: " + e.getMessage());
            throw new RuntimeException("Failed to start local resource pack server", e);
        }
    }

    public void stop() {
        running = false;
        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (dbConnection != null) {
            try {
                dbConnection.close();
                dbConnection = null;
            } catch (Exception e) {
                Bukkit.getLogger().severe("Error closing database connection: " + e.getMessage());
            }
        }
    }

    private void initializeDatabase() {
        try {
            Class.forName("org.sqlite.JDBC");
            dbConnection = DriverManager.getConnection("jdbc:sqlite:" + LocalServerConfig.getDatabaseFile().getAbsolutePath());
            
            // Create tables if they don't exist
            try (PreparedStatement stmt = dbConnection.prepareStatement(
                    "CREATE TABLE IF NOT EXISTS resource_packs (" +
                    "id TEXT PRIMARY KEY," +
                    "file_path TEXT NOT NULL," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)")) {
                stmt.executeUpdate();
            }
        } catch (Exception e) {
            Bukkit.getLogger().severe("Failed to initialize database: " + e.getMessage());
        }
    }

    public String storeResourcePack(File packFile) {
        String packId = UUID.randomUUID().toString();
        Path targetPath = LocalServerConfig.getResourcePacksPath().resolve(packId + ".zip");
        
        try {
            // Copy the pack to the server directory
            Files.copy(packFile.toPath(), targetPath);
            
            // Store in database
            try (PreparedStatement stmt = dbConnection.prepareStatement(
                    "INSERT INTO resource_packs (id, file_path) VALUES (?, ?)")) {
                stmt.setString(1, packId);
                stmt.setString(2, targetPath.toString());
                stmt.executeUpdate();
            }
            
            return packId;
        } catch (Exception e) {
            Bukkit.getLogger().severe("Failed to store resource pack: " + e.getMessage());
            return null;
        }
    }

    private class ResourcePackHandler implements HttpHandler {
        private String currentUUID = null;

        private String getCurrentOrGenerateUUID() {
            if (currentUUID == null) {
                currentUUID = UUID.randomUUID().toString();
            }
            return currentUUID;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String method = exchange.getRequestMethod();

            Bukkit.getLogger().info("Received " + method + " request for path: " + path);
            
            try {
                if (path.equals("/") || path.isEmpty()) {
                    handleInitialize(exchange);
                } else if (path.startsWith("/initialize")) {
                    handleInitialize(exchange);
                } else if (path.equals("/sha1")) {
                    handleSHA1Check(exchange);
                } else {
                    String packId = path.substring(1); // Remove leading slash
                    handleResourcePack(exchange, packId);
                }
            } catch (Exception e) {
                Bukkit.getLogger().severe("Error handling request: " + e.getMessage());
                e.printStackTrace();
                sendError(exchange, 500, "Internal server error: " + e.getMessage());
            } finally {
                exchange.close();
            }
        }

        private void handleSHA1Check(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equals("POST")) {
                sendError(exchange, 405, "Method not allowed");
                return;
            }

            // Parse the POST data
            String requestBody = new String(exchange.getRequestBody().readAllBytes());
            Map<String, String> params = parseFormData(requestBody);
            String uuid = params.get("uuid");
            String sha1 = params.get("sha1");

            if (uuid == null || sha1 == null) {
                sendError(exchange, 400, "Missing uuid or sha1 parameter");
                return;
            }

            try {
                // Check if we have this pack with this SHA1
                try (PreparedStatement stmt = dbConnection.prepareStatement(
                        "SELECT file_path FROM resource_packs WHERE id = ?")) {
                    stmt.setString(1, uuid);
                    ResultSet rs = stmt.executeQuery();
                    
                    JsonObject response = new JsonObject();
                    if (rs.next()) {
                        response.addProperty("success", true);
                        response.addProperty("uploadNeeded", false);
                        sendJsonResponse(exchange, 200, response.toString());
                    } else {
                        response.addProperty("success", true);
                        response.addProperty("uploadNeeded", true);
                        sendJsonResponse(exchange, 200, response.toString());
                    }
                }
            } catch (Exception e) {
                Bukkit.getLogger().severe("Error checking SHA1: " + e.getMessage());
                sendError(exchange, 500, "Internal server error");
            }
        }

        private void handleInitialize(HttpExchange exchange) throws IOException {
            // Return success with the current or new UUID
            String response = "{\"success\":true,\"uuid\":\"" + getCurrentOrGenerateUUID() + "\"}";
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }

        private void handleResourcePack(HttpExchange exchange, String packId) throws IOException {
            try {
                // Query database for pack
                try (PreparedStatement stmt = dbConnection.prepareStatement(
                        "SELECT file_path FROM resource_packs WHERE id = ?")) {
                    stmt.setString(1, packId);
                    ResultSet rs = stmt.executeQuery();
                    
                    if (rs.next()) {
                        String filePath = rs.getString("file_path");
                        File packFile = new File(filePath);
                        
                        if (packFile.exists()) {
                            Bukkit.getLogger().info("Serving resource pack: " + packId);
                            // Serve the file
                            exchange.getResponseHeaders().set("Content-Type", "application/zip");
                            exchange.sendResponseHeaders(200, packFile.length());
                            
                            try (FileInputStream fis = new FileInputStream(packFile);
                                 OutputStream os = exchange.getResponseBody()) {
                                byte[] buffer = new byte[8192];
                                int count;
                                while ((count = fis.read(buffer)) != -1) {
                                    os.write(buffer, 0, count);
                                }
                            }
                            Bukkit.getLogger().info("Successfully served resource pack: " + packId);
                        } else {
                            Bukkit.getLogger().warning("Pack file not found at path: " + filePath);
                            sendError(exchange, 404, "Resource pack file not found");
                        }
                    } else {
                        Bukkit.getLogger().warning("Invalid resource pack ID requested: " + packId);
                        sendError(exchange, 404, "Invalid resource pack ID");
                    }
                }
            } catch (Exception e) {
                Bukkit.getLogger().severe("Error serving resource pack: " + e.getMessage());
                e.printStackTrace();
                sendError(exchange, 500, "Internal server error");
            }
        }

        private Map<String, String> parseFormData(String data) {
            Map<String, String> result = new HashMap<>();
            for (String pair : data.split("&")) {
                String[] keyValue = pair.split("=");
                if (keyValue.length == 2) {
                    String key = URLDecoder.decode(keyValue[0], StandardCharsets.UTF_8);
                    String value = URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8);
                    result.put(key, value);
                }
            }
            return result;
        }

        private void sendJsonResponse(HttpExchange exchange, int code, String jsonString) throws IOException {
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            byte[] response = jsonString.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(code, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        }

        private void sendError(HttpExchange exchange, int code, String message) throws IOException {
            JsonObject error = new JsonObject();
            error.addProperty("error", message);
            sendJsonResponse(exchange, code, error.toString());
        }
    }
}