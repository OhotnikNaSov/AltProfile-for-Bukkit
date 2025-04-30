package sova.sAlts.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class AltUtils {

    public static boolean isAlt(Connection conn, String name) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT is_alt FROM nlogin WHERE last_name = ?")) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getBoolean("is_alt");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}
