package sova.sAlts;

import com.nickuc.login.api.nLoginAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import sova.sAlts.cache.AltCacheManager;
import sova.sAlts.commands.SubCommand;
import sova.sAlts.commands.SubCommandManager;
import sova.sAlts.config.ConfigManager;
import sova.sAlts.database.DatabaseManager;
import sova.sAlts.tab.SubCommandTabCompleter;

import java.util.Collections;

public class Main extends JavaPlugin {

    private static Main instance;

    private ConfigManager configManager;
    private SubCommandManager commandManager;
    private DatabaseManager databaseManager;
    private AltCacheManager altCacheManager;
    private nLoginAPI nLogin;

    private int altCacheTaskId = -1;

    @Override
    public void onEnable() {
        instance = this;

        this.configManager = new ConfigManager(this);
        this.commandManager = new SubCommandManager();
        this.databaseManager = new DatabaseManager();
        this.altCacheManager = new AltCacheManager();
        this.nLogin = nLoginAPI.getApi();

        // Подключение к базе
        this.databaseManager.connect();

        // Кеш альтов
        this.altCacheManager.loadCache();
        startAltCacheTask();

        // Команда и таб-комплитер
        getCommand("salts").setTabCompleter(new SubCommandTabCompleter(commandManager));

        getLogger().info("sAlts Plugin Enabled!");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.disconnect();
        }

        if (altCacheTaskId != -1) {
            Bukkit.getScheduler().cancelTask(altCacheTaskId);
        }

        getLogger().info("sAlts Plugin Disabled.");
    }

    public static Main getInstance() {
        return instance;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public SubCommandManager getCommandManager() {
        return commandManager;
    }

    public AltCacheManager getAltCacheManager() {
        return altCacheManager;
    }

    public nLoginAPI getnLogin() {
        return nLogin;
    }

    public void startAltCacheTask() {
        if (altCacheTaskId != -1) {
            Bukkit.getScheduler().cancelTask(altCacheTaskId);
        }

        int interval = getConfig().getInt("cache.refresh-interval-seconds", 300);

        altCacheTaskId = Bukkit.getScheduler().runTaskTimerAsynchronously(
                this,
                () -> {
                    altCacheManager.loadCache();
                    if(configManager.getDebugLogState())
                        getLogger().info("Кеш альтов обновлён. Всего: " + altCacheManager.getAllAltNames().size());
                },
                interval * 20L,
                interval * 20L
        ).getTaskId();

        if(configManager.getDebugLogState())
            getLogger().info("Автокеширование альтов запущено (интервал: " + interval + " сек)");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("salts")) return false;

        if (args.length == 0) {
            sender.sendMessage(configManager.getMessage("unknown_command", Collections.emptyMap()));
            return true;
        }

        SubCommand sub = commandManager.getSubCommand(args[0]);
        if (sub != null) {
            if (sub.getPermission() != null && !sender.hasPermission(sub.getPermission())) {
                sender.sendMessage(configManager.getMessage("no_permission", Collections.emptyMap()));
                return true;
            }
            sub.perform(sender, args);
        } else {
            sender.sendMessage(configManager.getMessage("unknown_command", Collections.emptyMap()));
        }

        return true;
    }
}
