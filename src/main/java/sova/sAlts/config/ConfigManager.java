package sova.sAlts.config;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigManager {
    private final JavaPlugin plugin;
    private FileConfiguration config;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public String getMessage(String path, Map<String, String> placeholders) {
        String raw = config.getString("messages." + path, "&cСообщение не найдено: " + path);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            raw = raw.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return translateColors(raw);
    }


    public String translateColors(String message) {
        // Перевод & в §
        message = ChatColor.translateAlternateColorCodes('&', message);

        // Поддержка HEX (пример: #FF00FF)
        Pattern hexPattern = Pattern.compile("#([A-Fa-f0-9]{6})");
        Matcher matcher = hexPattern.matcher(message);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String hexColor = matcher.group(1);
            String replacement = net.md_5.bungee.api.ChatColor.of("#" + hexColor).toString();
            matcher.appendReplacement(buffer, replacement);
        }
        matcher.appendTail(buffer);

        return buffer.toString();
    }

    // --- MySQL геттеры ---
    public String getMySQLHost() { return config.getString("mysql.host"); }
    public int getMySQLPort() { return config.getInt("mysql.port"); }
    public String getMySQLDatabase() { return config.getString("mysql.database"); }
    public String getMySQLUsername() { return config.getString("mysql.username"); }
    public String getMySQLPassword() { return config.getString("mysql.password"); }
}
