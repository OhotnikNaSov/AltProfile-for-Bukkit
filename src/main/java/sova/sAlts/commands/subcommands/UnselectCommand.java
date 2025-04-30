package sova.sAlts.commands.subcommands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import sova.sAlts.Main;
import sova.sAlts.commands.SubCommand;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class UnselectCommand extends SubCommand {

    @Override
    public String getName() {
        return "unselect";
    }

    @Override
    public String getDescription() {
        return "Убирает выбранного альта у игрока";
    }

    @Override
    public String getPermission() {
        return "salts.unselect";
    }

    @Override
    public String getUsage() {
        return "/salts unselect <player_name>";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("unsel");
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Main.getInstance().getConfigManager().getMessage("unselect_usage", Collections.emptyMap()));
            return;
        }

        String targetName = args[1];

        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            try (Connection conn = Main.getInstance().getDatabaseManager().getConnection()) {

                String currentAlt = null;

                // --- Проверяем, есть ли выбранный альт ---
                try (PreparedStatement check = conn.prepareStatement("SELECT selected_alt FROM nlogin WHERE last_name = ?")) {
                    check.setString(1, targetName);
                    try (ResultSet rs = check.executeQuery()) {
                        if (rs.next()) {
                            currentAlt = rs.getString("selected_alt");
                        } else {
                            String msg = Main.getInstance().getConfigManager().getMessage("unselect_player_not_found", Map.of("player", targetName));
                            Bukkit.getScheduler().runTask(Main.getInstance(), () -> sender.sendMessage(msg));
                            return;
                        }
                    }
                }

                if (currentAlt == null) {
                    String msg = Main.getInstance().getConfigManager().getMessage("unselect_none", Map.of("player", targetName));
                    Bukkit.getScheduler().runTask(Main.getInstance(), () -> sender.sendMessage(msg));
                    return;
                }

                // --- Удаляем выбранного альта ---
                try (PreparedStatement update = conn.prepareStatement("UPDATE nlogin SET selected_alt = NULL WHERE last_name = ?")) {
                    update.setString(1, targetName);
                    update.executeUpdate();
                }

                String finalCurrentAlt = currentAlt;
                Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                    Player altPlayer = Bukkit.getPlayer(finalCurrentAlt);

                    Map<String, String> placeholders = Map.of(
                            "alt_name", finalCurrentAlt,
                            "player", targetName
                    );

                    if (altPlayer != null && altPlayer.isOnline()) {
                        altPlayer.kickPlayer(Main.getInstance().getConfigManager().getMessage("unselect_success_alt", placeholders));
                    }

                    sender.sendMessage(Main.getInstance().getConfigManager().getMessage("unselect_success_admin", placeholders));
                });

            } catch (Exception e) {
                e.printStackTrace();
                Bukkit.getScheduler().runTask(Main.getInstance(), () ->
                        sender.sendMessage(Main.getInstance().getConfigManager().getMessage("unselect_error", Collections.emptyMap())));
            }
        });
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        }
        return Collections.emptyList();
    }
}
