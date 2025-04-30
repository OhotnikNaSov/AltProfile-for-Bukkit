package sova.sAlts.commands.subcommands;

import com.nickuc.login.api.types.Identity;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import sova.sAlts.Main;
import sova.sAlts.commands.SubCommand;

import java.sql.*;
import java.util.*;

public class CreateCommand extends SubCommand {

    @Override
    public String getName() {
        return "create";
    }

    @Override
    public String getDescription() {
        return "Создает альта!";
    }

    @Override
    public String getPermission() {
        return "salts.create";
    }

    @Override
    public String getUsage() {
        return "/salts create <alt_name> <real_name|null> <password>";
    }

    @Override
    public List<String> getAliases() {
        return Arrays.asList("cr", "new");
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage(Main.getInstance().getConfigManager().getMessage("create_usage", Collections.emptyMap() ));
            return;
        }

        String altName = args[1];
        String realName = args[2];
        String password = args[3];

        Bukkit.getScheduler().runTaskAsynchronously(Main.getInstance(), () -> {
            UUID uuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + altName).getBytes());

            try (Connection conn = Main.getInstance().getDatabaseManager().getConnection()) {

                // --- 1. Добавление в alt_profiles ---
                try (PreparedStatement ps1 = conn.prepareStatement("""
                    INSERT INTO alt_profiles (alt_name, uuid, real_name)
                    VALUES (?, ?, ?)
                """)) {
                    ps1.setString(1, altName);
                    ps1.setString(2, uuid.toString());

                    if (realName.equalsIgnoreCase("null")) {
                        ps1.setNull(3, Types.VARCHAR);
                    } else {
                        ps1.setString(3, realName.trim());
                    }

                    ps1.executeUpdate();
                }

                // --- 2. Регистрация через nLogin ---
                try {
                    Main.getInstance().getnLogin().performRegister(Identity.ofOffline(altName), password, "127.0.0.1");
                } catch (Exception e) {
                    Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                        Map<String, String> errorPlaceholders = Map.of("error", e.getMessage());
                        sender.sendMessage(Main.getInstance().getConfigManager().getMessage("nlogin_error", errorPlaceholders));
                    });
                    return;
                }

                // --- 3. Получаем ID альта ---
                int altId = -1;
                try (PreparedStatement ps = conn.prepareStatement("SELECT id FROM alt_profiles WHERE alt_name = ?")) {
                    ps.setString(1, altName);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            altId = rs.getInt("id");
                        }
                    }
                }

                if (altId == -1) {
                    Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                        sender.sendMessage(Main.getInstance().getConfigManager().getMessage("alt_not_found", Map.of("name", altName)));
                    });
                    return;
                }

                // --- 4. Привязываем altId к владельцу (если указан) ---
                if (!realName.equalsIgnoreCase("null")) {
                    try (PreparedStatement ps = conn.prepareStatement("""
                        UPDATE nlogin
                        SET available_alts = 
                            CASE
                                WHEN available_alts IS NULL OR available_alts = '' THEN JSON_ARRAY(?)
                                ELSE JSON_ARRAY_APPEND(available_alts, '$', ?)
                            END
                        WHERE last_name = ?;
                    """)) {
                        ps.setInt(1, altId);
                        ps.setInt(2, altId);
                        ps.setString(3, realName.trim());
                        ps.executeUpdate();
                    }
                }

                // --- 5. Обновляем is_alt ---
                try (PreparedStatement ps = conn.prepareStatement("""
                    UPDATE nlogin
                    SET is_alt = 1
                    WHERE last_name = ?;
                """)) {
                    ps.setString(1, altName);
                    ps.executeUpdate();
                }

                // --- 6. Ответ игроку ---
                final int finalAltId = altId;
                Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                    Main.getInstance().getAltCacheManager().loadCache();

                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("name", altName);
                    placeholders.put("real_name", realName);
                    placeholders.put("alt_id", String.valueOf(finalAltId));
                    placeholders.put("owner_info", realName.equalsIgnoreCase("null") ? "" : " &7для &b" + realName);

                    sender.sendMessage(Main.getInstance().getConfigManager().getMessage("create_success", placeholders));
                });

            } catch (SQLException e) {
                e.printStackTrace();
                Bukkit.getScheduler().runTask(Main.getInstance(), () -> {
                    sender.sendMessage(Main.getInstance().getConfigManager().getMessage("sql_error", Map.of("error", e.getMessage())));
                });
            }
        });
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) return Collections.singletonList("<alt_name>");
        if (args.length == 3) return Arrays.asList("<real_name>", "null");
        if (args.length == 4) return Collections.singletonList("<password>");
        return Collections.emptyList();
    }
}
