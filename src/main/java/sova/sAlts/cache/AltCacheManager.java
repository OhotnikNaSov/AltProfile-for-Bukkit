package sova.sAlts.cache;

import sova.sAlts.Main;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AltCacheManager {

    private final Map<String, AltData> altMap = new ConcurrentHashMap<>();

    public void loadCache() {
        altMap.clear();
        try (Connection conn = Main.getInstance().getDatabaseManager().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT alt_name, real_name FROM alt_profiles");
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String altName = rs.getString("alt_name");
                String realName = rs.getString("real_name"); // может быть null
                altMap.put(altName.toLowerCase(), new AltData(altName, realName));
            }

        } catch (Exception e) {
            if( Main.getInstance().getConfigManager().getDebugLogState() )
                Main.getInstance().getLogger().warning("❌ Не удалось загрузить кеш альтов: " + e.getMessage());
        }
    }

    public void addAlt(String altName, String realName) {
        altMap.put(altName.toLowerCase(), new AltData(altName, realName));
    }

    public void removeAlt(String altName) {
        altMap.remove(altName.toLowerCase());
    }

    public AltData getAlt(String altName) {
        return altMap.get(altName.toLowerCase());
    }

    public Set<String> getAllAltNames() {
        Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (AltData data : altMap.values()) {
            names.add(data.getAltName());
        }
        return names;
    }

    public Set<String> getAltsOwnedBy(String playerName) {
        Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (AltData data : altMap.values()) {
            if (playerName.equalsIgnoreCase(data.getRealName())) {
                names.add(data.getAltName());
            }
        }
        return names;
    }

    public boolean contains(String altName) {
        return altMap.containsKey(altName.toLowerCase());
    }

    public boolean isEmpty() {
        return altMap.isEmpty();
    }
}
