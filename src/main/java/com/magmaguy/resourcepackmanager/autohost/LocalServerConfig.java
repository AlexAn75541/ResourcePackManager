package com.magmaguy.resourcepackmanager.autohost;

import lombok.Getter;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LocalServerConfig {
    @Getter
    private static final int PORT = 50000;
    @Getter
    private static final String HOST = "localhost";
    
    @Getter
    private static File serverDirectory;
    @Getter
    private static File databaseFile;
    
    public static void initialize(org.bukkit.plugin.java.JavaPlugin plugin) {
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