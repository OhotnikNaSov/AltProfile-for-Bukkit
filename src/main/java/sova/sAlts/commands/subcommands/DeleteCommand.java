package sova.sAlts.commands.subcommands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import sova.sAlts.Main;
import sova.sAlts.cache.AltData;
import sova.sAlts.commands.SubCommand;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.*;

public class DeleteCommand extends SubCommand {

    @Override
    public String getName() {
        return "delete";
    }

    @Override
    public String getDescription() {
        return "Удаляет альта из базы";
    }

    @Override
    public String getPermission() {
        return "salts.delete";
    }

    @Override
    public String getUsage() {
        return "/salts delete <alt_name>";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("del");
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Main.getInstance().getConfigManager().getMessage("delete_usage", Collections.emptyMap()));
            return;
        }

        String altName = args[1];
        var cache = Main.getInstance().getAltCacheManager();

        if (cache.isEmpty()) {
            sender.sendMessage(Main.getInstance().getConfigManager().getMessage("delete_no_alts", Collections.emptyMap()));
            return;
        }

        AltData altData = cache.getAlt(altName);
        if (altData == null) {
            sender.sendMessage(Main.getInstance().getConfigManager().getMessage("delete_not_found", Map.of("alt_name", altName)));
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            try (Connection conn = Main.getInstance().getDatabaseManager().getConnection()) {

                // --- 1. Удаляем из alt_profiles ---
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM alt_profiles WHERE alt_name = ?")) {
                    ps.setString(1, altName);
                    ps.executeUpdate();
                }

                // --- 2. Удаляем из nlogin, если is_alt = 1 ---
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM nlogin WHERE last_name = ? AND is_alt = 1")) {
                    ps.setString(1, altName);
                    ps.executeUpdate();
                }

                // --- 3. Обновляем selected_alt = NULL у владельцев ---
                try (PreparedStatement ps = conn.prepareStatement("UPDATE nlogin SET selected_alt = NULL WHERE selected_alt = ?")) {
                    ps.setString(1, altName);
                    ps.executeUpdate();
                }

                // --- 4. Удаляем из кэша и сообщаем игроку ---
                cache.removeAlt(altName);

                Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                    sender.sendMessage(Main.getInstance().getConfigManager().getMessage("delete_success", Map.of("alt_name", altName)));
                });

            } catch (Exception e) {
                e.printStackTrace();
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("alt_name", altName);
                placeholders.put("error", e.getMessage());

                Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                    sender.sendMessage(Main.getInstance().getConfigManager().getMessage("delete_sql_error", placeholders));
                });
            }
        });
    }


    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return new ArrayList<>(Main.getInstance().getAltCacheManager().getAllAltNames());
        }
        return Collections.emptyList();
    }
}
