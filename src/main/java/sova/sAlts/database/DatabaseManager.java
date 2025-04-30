package sova.sAlts.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import sova.sAlts.Main;

import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseManager {
    private HikariDataSource dataSource;

    public void connect() {
        try {
            Main main = Main.getInstance();
            String host = main.getConfigManager().getMySQLHost();
            int port = main.getConfigManager().getMySQLPort();
            String database = main.getConfigManager().getMySQLDatabase();
            String username = main.getConfigManager().getMySQLUsername();
            String password = main.getConfigManager().getMySQLPassword();

            String jdbcUrl = "jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&autoReconnect=true&characterEncoding=utf8";

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(username);
            config.setPassword(password);
            config.setMaximumPoolSize(10); // Настраивай под нагрузку
            config.setMinimumIdle(2);
            config.setIdleTimeout(30000);
            config.setMaxLifetime(1800000);
            config.setConnectionTimeout(10000);
            config.setLeakDetectionThreshold(20000);
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            dataSource = new HikariDataSource(config);
            if( main.getConfigManager().getDebugLogState() )
                main.getLogger().info("Подключение к MySQL успешно установлено!");
        } catch (Exception e) {
            if( Main.getInstance().getConfigManager().getDebugLogState() )
                Main.getInstance().getLogger().warning("Не удалось подключиться к MySQL: " + e.getMessage());
        }
    }

    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            if( Main.getInstance().getConfigManager().getDebugLogState() )
                Main.getInstance().getLogger().info("Подключение к MySQL было закрыто.");
        }
    }

    public void reconnect() {
        disconnect();
        connect();
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            connect();
        }
        return dataSource.getConnection();
    }

    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }
}
