package com.magmaguy.resourcepackmanager.autohost;

import com.magmaguy.resourcepackmanager.ResourcePackManager;
import com.magmaguy.resourcepackmanager.config.DefaultConfig;
import lombok.Getter;
import org.bukkit.Bukkit;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LocalServerConfig {
    @Getter
    private static int port;
    @Getter
    private static String host;
    
    public static void setPort(int newPort) {
        port = newPort;
        Bukkit.getLogger().warning("Server port changed to " + newPort + ". Please update your config.yml with this new port!");
    }
    
    @Getter
    private static File serverDirectory;
    @Getter
    private static File databaseFile;
    
    public static void initialize(org.bukkit.plugin.java.JavaPlugin plugin) {
        // Load configuration
        host = DefaultConfig.getServerUrl();
        port = DefaultConfig.getServerPort();

        // Create server directory under plugin's data folder
        serverDirectory = new File(plugin.getDataFolder(), "localserver");
        if (!serverDirectory.exists()) {
            serverDirectory.mkdirs();
        }
        
        // Initialize database file
        databaseFile = new File(serverDirectory, "packs.db");
    }
    
    public static Path getResourcePacksPath() {
        return Paths.get(serverDirectory.getPath(), "packs");
    }
}