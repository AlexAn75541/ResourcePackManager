package com.magmaguy.resourcepackmanager.autohost;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import lombok.Getter;
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

    public LocalResourcePackServer() {
        instance = this;
    }

    public void start() {
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

            Bukkit.getLogger().info("Local resource pack server started on http://" + 
                       LocalServerConfig.getHost() + ":" + 
                       LocalServerConfig.getPort() + "/");
        } catch (IOException e) {
            Bukkit.getLogger().severe("Failed to start local resource pack server: " + e.getMessage());
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
        if (dbConnection != null) {
            try {
                dbConnection.close();
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
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            String packId = path.substring(1); // Remove leading slash
            
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
                        } else {
                            sendError(exchange, 404, "Resource pack not found");
                        }
                    } else {
                        sendError(exchange, 404, "Invalid resource pack ID");
                    }
                }
            } catch (Exception e) {
                Bukkit.getLogger().severe("Error serving resource pack: " + e.getMessage());
                sendError(exchange, 500, "Internal server error");
            } finally {
                exchange.close();
            }
        }

        private void sendError(HttpExchange exchange, int code, String message) throws IOException {
            exchange.sendResponseHeaders(code, message.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(message.getBytes());
            }
        }
    }
}