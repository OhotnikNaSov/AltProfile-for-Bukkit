package sova.sAlts.commands.subcommands;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.*;
import org.bukkit.command.CommandSender;
import sova.sAlts.Main;
import sova.sAlts.cache.AltData;
import sova.sAlts.commands.SubCommand;

import java.util.*;

public class ListCommand extends SubCommand {

    private final int ITEMS_PER_PAGE = 5;

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public String getDescription() {
        return "Показывает список всех альтов";
    }

    @Override
    public String getPermission() {
        return "salts.list";
    }

    @Override
    public String getUsage() {
        return "/salts list <page>";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("ls");
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        List<AltData> altList = new ArrayList<>(Main.getInstance().getAltCacheManager().getAllAltNames().stream()
                .map(name -> Main.getInstance().getAltCacheManager().getAlt(name))
                .toList());

        if (altList.isEmpty()) {
            sender.sendMessage(Main.getInstance().getConfigManager().getMessage("list_empty",  Collections.emptyMap() ));
            return;
        }

        int page = 1;
        if (args.length >= 2) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignored) {
                sender.sendMessage(Main.getInstance().getConfigManager().getMessage("list_invalid_page",  Collections.emptyMap() ));
                return;
            }
        }

        int totalPages = (int) Math.ceil((double) altList.size() / ITEMS_PER_PAGE);
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;

        int start = (page - 1) * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, altList.size());

        Map<String, String> headerVars = Map.of("page", String.valueOf(page), "pages", String.valueOf(totalPages));
        sender.sendMessage("");
        sender.sendMessage(Main.getInstance().getConfigManager().getMessage("list_header", headerVars));

        for (int i = start; i < end; i++) {
            AltData alt = altList.get(i);
            String altName = alt.getAltName();
            String realName = alt.getRealName() != null ? alt.getRealName() : "никто";

            Map<String, String> vars = new HashMap<>();
            vars.put("alt_name", altName);
            vars.put("owner", realName);

            String rawFormatted = Main.getInstance().getConfigManager().getMessage("list_format", vars);

            TextComponent line = new TextComponent(TextComponent.fromLegacyText(rawFormatted));
            line.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/salts delete " + altName));
            line.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    new ComponentBuilder(Main.getInstance().getConfigManager().translateColors(
                            Main.getInstance().getConfigManager().getMessage("list_hover",  Collections.emptyMap())
                    )).color(ChatColor.RED).create()));

            sender.spigot().sendMessage(line);
        }

        sender.sendMessage(Main.getInstance().getConfigManager().getMessage("list_footer",  Collections.emptyMap()));
        sender.sendMessage("");
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return Collections.singletonList("<page>");
        }
        return Collections.emptyList();
    }
}
