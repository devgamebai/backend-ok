/*
 * Decompiled with CFR 0.152.
 */
package game.third.usecase.game568win.entities;

public enum ProductType {
    SPORTS_BOOK(1, "Sports Book"),
    SBO_GAMES(3, "SBO Games"),
    VIRTUAL_SPORTS(5, "Virtual Sports"),
    SBO_LIVE_CASINO(7, "SBO Live Casino"),
    SEAMLESS_GAME_PROVIDER(9, "Seamless Game Provider");

    private final int id;
    private final String productName;

    private ProductType(int id, String productName) {
        this.id = id;
        this.productName = productName;
    }

    public static ProductType getById(int productType) {
        for (ProductType type : ProductType.values()) {
            if (type.getId() != productType) continue;
            return type;
        }
        return null;
    }

    public int getId() {
        return this.id;
    }

    public String getProductName() {
        return this.productName;
    }
}

