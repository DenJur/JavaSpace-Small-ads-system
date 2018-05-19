package app.Util;

/**
 * Enum of all possible ads. Used in comboboxes for ad type selection.
 */
public enum AdTypes {

    BUY("Buyout"),
    BID("Auction"),
    ANY("Any");

    private String displayName;

    AdTypes(String displayName) {
        this.displayName = displayName;
    }

    public static AdTypes fromString(String text) {
        for (AdTypes b : AdTypes.values()) {
            if (b.displayName.equalsIgnoreCase(text)) {
                return b;
            }
        }
        return null;
    }

    public String displayName() {
        return displayName;
    }

    // Optionally and/or additionally, toString.
    @Override
    public String toString() {
        return displayName;
    }
}
