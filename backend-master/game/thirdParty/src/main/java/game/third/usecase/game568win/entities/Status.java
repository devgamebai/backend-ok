/*
 * Decompiled with CFR 0.152.
 */
package game.third.usecase.game568win.entities;

public enum Status {
    Running("Running"),
    Settled("Settled"),
    Void("Void"),
    Bonus("Bonus"),
    Cancel("Cancel"),
    Rollback("Rollback");

    private final String value;

    private Status(String value) {
        this.value = value;
    }

    public String toString() {
        return this.value;
    }

    public static Status fromString(String text) {
        for (Status b : Status.values()) {
            if (!b.value.equalsIgnoreCase(text)) continue;
            return b;
        }
        return null;
    }
}

