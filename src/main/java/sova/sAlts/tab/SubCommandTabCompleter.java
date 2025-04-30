package sova.sAlts.tab;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import sova.sAlts.commands.SubCommand;
import sova.sAlts.commands.SubCommandManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SubCommandTabCompleter implements TabCompleter {

    private final SubCommandManager manager;

    public SubCommandTabCompleter(SubCommandManager manager) {
        this.manager = manager;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String input = args[0].toLowerCase();

            for (SubCommand sub : manager.getSubCommands()) {
                String permission = sub.getPermission();

                if (permission != null && !sender.hasPermission(permission)) {
                    continue; // нет прав — не показываем
                }

                if (sub.getName().toLowerCase().startsWith(input)) {
                    completions.add(sub.getName());
                }
            }

            return completions;
        }

        if (args.length > 1) {
            SubCommand sub = manager.getSubCommand(args[0]);
            if (sub != null) {
                String permission = sub.getPermission();
                if (permission == null || sender.hasPermission(permission)) {
                    return sub.tabComplete(sender, args);
                }
            }
        }

        return Collections.emptyList();
    }
}
