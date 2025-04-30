package sova.sAlts.commands.subcommands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import sova.sAlts.Main;
import sova.sAlts.cache.AltData;
import sova.sAlts.commands.SubCommand;
import sova.sAlts.util.AltUtils;

import java.sql.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SwitchCommand extends SubCommand {

    @Override
    public String getName() {
        return "switch";
    }

    @Override
    public String getDescription() {
        return "Выбирает альта или снимает его";
    }

    @Override
    public String getPermission() {
        return "salts.switch";
    }

    @Override
    public String getUsage() {
        return "/salts switch <alt_name>";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("sw");
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Main.getInstance().getConfigManager().getMessage("switch_only_player", Collections.emptyMap() ));
            return;
        }

        // --- /salts switch — снять альта
        if (args.length == 1) {
            Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
                try (Connection conn = Main.getInstance().getDatabaseManager().getConnection()) {
                    String lookupName = resolveRealOwner(conn, player.getName());

                    try (PreparedStatement check = conn.prepareStatement("SELECT selected_alt FROM nlogin WHERE last_name = ?")) {
                        check.setString(1, lookupName);
                        try (ResultSet rs = check.executeQuery()) {
                            if (rs.next()) {
                                String selected = rs.getString("selected_alt");

                                if (selected == null) {
                                    Bukkit.getScheduler().runTask(Main.getInstance(), () ->
                                            sender.sendMessage(Main.getInstance().getConfigManager().getMessage("switch_none_selected", Collections.emptyMap() )));
                                    return;
                                }

                                try (PreparedStatement clear = conn.prepareStatement("UPDATE nlogin SET selected_alt = NULL WHERE last_name = ?")) {
                                    clear.setString(1, lookupName);
                                    clear.executeUpdate();
                                }

                                String finalSelected = selected;
                                Bukkit.getScheduler().runTask(Main.getInstance(), () ->
                                        player.kickPlayer(Main.getInstance().getConfigManager().getMessage("switch_success_unselect",
                                                Map.of("alt_name", finalSelected)))
                                );

                            } else {
                                Bukkit.getScheduler().runTask(Main.getInstance(), () ->
                                        sender.sendMessage(Main.getInstance().getConfigManager().getMessage("switch_not_found_in_db", Collections.emptyMap() )));
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Bukkit.getScheduler().runTask(Main.getInstance(), () ->
                            sender.sendMessage(Main.getInstance().getConfigManager().getMessage("switch_remove_error", Collections.emptyMap() )));
                }
            });
            return;
        }

        // --- /salts switch <alt_name>
        String altName = args[1];
        var cache = Main.getInstance().getAltCacheManager();

        if (!cache.contains(altName)) {
            sender.sendMessage(Main.getInstance().getConfigManager().getMessage("switch_alt_not_found", Map.of("alt_name", altName)));
            return;
        }

        AltData alt = cache.getAlt(altName);

        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            try (Connection conn = Main.getInstance().getDatabaseManager().getConnection()) {

                // Запрещаем альту переключаться
                if (AltUtils.isAlt(conn, player.getName())) {
                    Bukkit.getScheduler().runTask(Main.getInstance(), () ->
                            sender.sendMessage(Main.getInstance().getConfigManager().getMessage("switch_already_as_alt", Collections.emptyMap() )));
                    return;
                }

                // Неадмин → можно только на своих
                if (!sender.hasPermission("salts.switch.admin")) {
                    if (alt.getRealName() == null || !alt.getRealName().equalsIgnoreCase(player.getName())) {
                        Bukkit.getScheduler().runTask(Main.getInstance(), () ->
                                sender.sendMessage(Main.getInstance().getConfigManager().getMessage("switch_no_permission_alt", Collections.emptyMap() )));
                        return;
                    }
                }

                // Проверка, занят ли альт
                try (PreparedStatement check = conn.prepareStatement("SELECT last_name FROM nlogin WHERE selected_alt = ?")) {
                    check.setString(1, altName);
                    try (ResultSet rs = check.executeQuery()) {
                        if (rs.next()) {
                            String takenBy = rs.getString("last_name");
                            Bukkit.getScheduler().runTask(Main.getInstance(), () ->
                                    sender.sendMessage(Main.getInstance().getConfigManager().getMessage("switch_alt_taken", Map.of("taken_by", takenBy))));
                            return;
                        }
                    }
                }

                // Применяем альта
                String lookupName = resolveRealOwner(conn, player.getName());

                try (PreparedStatement update = conn.prepareStatement("UPDATE nlogin SET selected_alt = ? WHERE last_name = ?")) {
                    update.setString(1, altName);
                    update.setString(2, lookupName);
                    update.executeUpdate();
                }

                Bukkit.getScheduler().runTask(Main.getInstance(), () ->
                        player.kickPlayer(Main.getInstance().getConfigManager().getMessage("switch_success", Map.of("alt_name", altName)))
                );

            } catch (Exception e) {
                e.printStackTrace();
                Bukkit.getScheduler().runTask(Main.getInstance(), () ->
                        sender.sendMessage(Main.getInstance().getConfigManager().getMessage("switch_error", Collections.emptyMap() )));
            }
        });
    }

    private String resolveRealOwner(Connection conn, String name) {
        try (PreparedStatement altCheck = conn.prepareStatement("SELECT is_alt FROM nlogin WHERE last_name = ?")) {
            altCheck.setString(1, name);
            try (ResultSet rs = altCheck.executeQuery()) {
                if (rs.next() && rs.getBoolean("is_alt")) {
                    try (PreparedStatement ownerLookup = conn.prepareStatement("SELECT real_name FROM alt_profiles WHERE alt_name = ?")) {
                        ownerLookup.setString(1, name);
                        try (ResultSet rsOwner = ownerLookup.executeQuery()) {
                            if (rsOwner.next()) {
                                return rsOwner.getString("real_name");
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return name;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) return Collections.emptyList();

        if (args.length == 2) {
            Set<String> result;

            if (sender.hasPermission("salts.switch.admin")) {
                result = Main.getInstance().getAltCacheManager().getAllAltNames();
            } else {
                result = Main.getInstance().getAltCacheManager().getAltsOwnedBy(player.getName());
            }

            return result.stream()
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        }

        return Collections.emptyList();
    }
}
