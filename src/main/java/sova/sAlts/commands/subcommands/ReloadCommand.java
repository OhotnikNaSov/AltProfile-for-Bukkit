package sova.sAlts.commands.subcommands;

import org.bukkit.command.CommandSender;
import sova.sAlts.Main;
import sova.sAlts.commands.SubCommand;

import java.util.Collections;
import java.util.List;

public class ReloadCommand extends SubCommand {

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getDescription() {
        return "Перезагружает конфиг";
    }

    @Override
    public String getUsage() {
        return "/salts reload";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("rl");
    }

    @Override
    public String getPermission() {
        return "salts.reload";
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        try {
            Main.getInstance().getConfigManager().reload();

            Main.getInstance().getDatabaseManager().reconnect();
            Main.getInstance().getAltCacheManager().loadCache();
            Main.getInstance().startAltCacheTask();

            sender.sendMessage(Main.getInstance().getConfigManager().getMessage("reload_success", Collections.emptyMap() ));

            if( Main.getInstance().getConfigManager().getDebugLogState() )
                Main.getInstance().getLogger().info("Конфигурация успешно перезагружена! И DebugLOG был включен!");
        } catch (Exception e) {
            e.printStackTrace();
            sender.sendMessage(Main.getInstance().getConfigManager().getMessage("reload_error",
                    Collections.singletonMap("error", e.getMessage())));
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
