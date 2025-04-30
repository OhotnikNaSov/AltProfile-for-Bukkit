package sova.sAlts.commands;

import org.bukkit.command.CommandSender;

import java.util.List;


public abstract class SubCommand {
    public abstract String getName();
    public abstract String getDescription();
    public abstract String getUsage();
    public abstract List<String> getAliases();

    public abstract String getPermission();

    public abstract void perform(CommandSender sender, String[] args);
    public abstract List<String> tabComplete(CommandSender sender, String[] args);
}
