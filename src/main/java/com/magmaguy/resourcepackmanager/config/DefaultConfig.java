package com.magmaguy.resourcepackmanager.config;

import com.magmaguy.magmacore.config.ConfigurationEngine;
import com.magmaguy.magmacore.config.ConfigurationFile;
import lombok.Getter;

import java.util.List;

public class DefaultConfig extends ConfigurationFile {

    @Getter
    private static List<String> priorityOrder;
    @Getter
    private static boolean autoHost;
    @Getter
    private static boolean forceResourcePack;
    @Getter
    private static String resourcePackPrompt;
    @Getter
    private static String resourcePackRerouting;
    @Getter
    private static String serverUrl;
    @Getter
    private static int serverPort;
    @Getter
    private static boolean useLocalServer;


    public DefaultConfig() {
        super("config.yml");
    }

    @Override
    public void initializeValues() {
        priorityOrder = ConfigurationEngine.setList(
                List.of(
                        "Sets the list, from highest priority (top) to lowest priority (bottom), in which the resource" +
                                " packs will automatically resolve merge conflicts.",
                        "The defaults use plugin names. If you manually added your own resource pack in the mixer folder to be merged in, add its exact filename, including .zip in the name"),
                fileConfiguration, "priorityOrder",
                List.of(
                        "ResourcePackManager",
                        "EliteMobs",
                        "FreeMinecraftModels",
                        "ModelEngine",
                        "Nova",
                        "ItemsAdder",
                        "Oraxen",
                        "BetterHUD",
                        "ValhallaMMO",
                        "MMOInventory",
                        "vane-core",
                        "RealisticSurvival"));

        autoHost = ConfigurationEngine.setBoolean(
                List.of("Automatically host the resource pack on MagmaGuy's servers",
                        "These servers cost money to keep running. There is no guarantee this will be an option forever."),
                fileConfiguration, "autoHost", true);

        forceResourcePack = ConfigurationEngine.setBoolean(
                List.of("Sets whether the resource pack use will be forced to clients"),
                fileConfiguration, "forceResourcePack", false);
        resourcePackPrompt = ConfigurationEngine.setString(
                List.of("Sets whether the resource pack use will be forced to clients"),
                fileConfiguration, "resourcePackPrompt", "Use recommended resource pack?");
        resourcePackRerouting = ConfigurationEngine.setString(
                List.of(
                        "OPTIONAL: Copy the merged directory to a custom directory location. Useful for unusual setups, like people trying to host with a different plugin.",
                        "If you are hosting with a different plugin make sure to disable Auto-hosting here!",
                        "This will use the plugin directory as the base directory.",
                        "As an example, if you wanted to target ResourcePackManager's output folder, you'd do:",
                        "ResourcePackManage/output",
                        "If you don't know what any of what is written here means, just don't touch this setting!"),
                fileConfiguration, "resourcePackRerouting", "");

        useLocalServer = ConfigurationEngine.setBoolean(
                List.of("Whether to use the local server for hosting resource packs instead of magmaguy.com",
                        "If true, the plugin will host resource packs using the built-in server"),
                fileConfiguration, "useLocalServer", false);

        serverUrl = ConfigurationEngine.setString(
                List.of("The URL where your resource pack server will be accessible",
                        "Default is localhost, change this to your server's public IP or domain if you want to make it accessible externally",
                        "Don't include protocol (http://) or port number"),
                fileConfiguration, "serverUrl", "localhost");

        serverPort = ConfigurationEngine.setInt(
                List.of("The port number for the resource pack server",
                        "Make sure this port is open in your firewall if you want to make it accessible externally"),
                fileConfiguration, "serverPort", 50000);
    }
}
