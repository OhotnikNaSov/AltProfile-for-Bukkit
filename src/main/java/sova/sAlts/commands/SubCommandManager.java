package sova.sAlts.commands;

import sova.sAlts.commands.subcommands.*;

import java.util.ArrayList;
import java.util.List;

public class SubCommandManager {
    private final List<SubCommand> subCommands = new ArrayList<>();

    public SubCommandManager() {
        subCommands.add(new CreateCommand());
        subCommands.add(new ReloadCommand());
        subCommands.add(new DbInitCommand());
        subCommands.add(new DeleteCommand());
        subCommands.add(new SwitchCommand());
        subCommands.add(new UnselectCommand());
        subCommands.add(new ListCommand());

    }

    public List<SubCommand> getSubCommands() {
        return subCommands;
    }

    public SubCommand getSubCommand(String name) {
        for (SubCommand sub : subCommands) {
            if (sub.getName().equalsIgnoreCase(name) || sub.getAliases().contains(name.toLowerCase())) {
                return sub;
            }
        }
        return null;
    }
}
