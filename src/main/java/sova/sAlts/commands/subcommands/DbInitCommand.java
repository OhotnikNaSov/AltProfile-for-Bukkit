package sova.sAlts.commands.subcommands;

import org.bukkit.command.CommandSender;
import sova.sAlts.Main;
import sova.sAlts.commands.SubCommand;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DbInitCommand extends SubCommand {

    @Override
    public String getName() {
        return "db_init";
    }

    @Override
    public String getDescription() {
        return "Инициализирует структуру базы данных для альтов";
    }

    @Override
    public String getUsage() {
        return "/salts db_init";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("dbi");
    }

    @Override
    public String getPermission() {
        return "salts.db_init";
    }

    @Override
    public void perform(CommandSender sender, String[] args) {
        try (Connection conn = Main.getInstance().getDatabaseManager().getConnection();
             Statement stmt = conn.createStatement()) {

            // ========== 1. nlogin ==========
            stmt.executeUpdate("""
                ALTER TABLE nlogin ADD COLUMN IF NOT EXISTS available_alts TEXT DEFAULT NULL;
            """);

            stmt.executeUpdate("""
                ALTER TABLE nlogin ADD COLUMN IF NOT EXISTS selected_alt VARCHAR(64) DEFAULT NULL;
            """);

            stmt.executeUpdate("""
                ALTER TABLE nlogin ADD COLUMN IF NOT EXISTS is_alt TINYINT(1) DEFAULT 0;
            """);

            try {
                stmt.executeUpdate("""
                    CREATE UNIQUE INDEX IF NOT EXISTS idx_selected_alt_unique ON nlogin (selected_alt);
                """);
            } catch (SQLException e) {
                if (!e.getMessage().contains("already exists")) {
                    Map<String, String> indexError = Map.of("error", e.getMessage());
                    Main.getInstance().getLogger().warning(
                            Main.getInstance().getConfigManager().getMessage("db_init_index_warning", indexError)
                    );
                }
            }

            // ========== 2. alt_profiles ==========
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS alt_profiles (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    alt_name VARCHAR(32) NOT NULL UNIQUE,
                    uuid VARCHAR(64) NOT NULL UNIQUE,
                    real_name VARCHAR(32),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
            """);

            sender.sendMessage(Main.getInstance().getConfigManager().getMessage("db_init_success", Collections.emptyMap() ));

        } catch (SQLException e) {
            e.printStackTrace();
            Map<String, String> error = Map.of("error", e.getMessage());
            sender.sendMessage(Main.getInstance().getConfigManager().getMessage("db_init_error", error));
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
