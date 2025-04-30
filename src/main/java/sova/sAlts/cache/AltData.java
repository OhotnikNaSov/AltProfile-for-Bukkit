package sova.sAlts.cache;

public class AltData {
    private final String altName;
    private final String realName; // может быть null

    public AltData(String altName, String realName) {
        this.altName = altName;
        this.realName = realName;
    }

    public String getAltName() {
        return altName;
    }

    public String getRealName() {
        return realName;
    }
}
