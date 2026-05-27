/*
 * Decompiled with CFR 0.152.
 */
package game.modules.minigame;

import game.modules.minigame.GroupItemType;

public class GroupItem {
    public static final int DIAMOND_TOTAL_ITEM = 9;
    public static final int DIAMOND_NUMBER_ITEM_ONE_LINE = 3;
    public static final long DIAMOND_PRIZE_AMOUNT_THREE_VIOLET = 85L;
    public static final long DIAMOND_PRIZE_AMOUNT_THREE_BLUE = 40L;
    public static final long DIAMOND_PRIZE_AMOUNT_THREE_WHITE = 20L;
    public static final long DIAMOND_PRIZE_AMOUNT_THREE_GREEN = 8L;
    public static final long DIAMOND_PRIZE_AMOUNT_TWO_GREEN = 0L;
    public static final long DIAMOND_PRIZE_AMOUNT_THREE_RED = 3L;
    public static final long DIAMOND_PRIZE_AMOUNT_TWO_RED = 0L;
    public static final long DIAMOND_PRIZE_AMOUNT_FAIL = 0L;
    public static final long DIAMOND_NEW_PRIZE_AMOUNT_THREE_RED = 10L;
    public static final long DIAMOND_NEW_PRIZE_AMOUNT_THREE_GREEN = 1L;
    public static final long DIAMOND_NEW_PRIZE_AMOUNT_THREE_WHITE = 0L;
    public static final long DIAMOND_NEW_PRIZE_AMOUNT_THREE_BLUE = 0L;
    private int[] items;
    private GroupItemType groupItemType;
    private long prizeAmount;
    private boolean isJackpot;
    private int mutil = 1;

    public GroupItem(int[] items) {
        this.items = items;
        this.groupItemType = this.getGroupItemType();
    }

    public GroupItem(int[] items, int mutil) {
        this.items = items;
        this.mutil = mutil;
        this.groupItemType = this.getGroupItemTypeNew();
    }

    public long getPrizeAmount() {
        return this.prizeAmount;
    }

    public boolean isJackpot() {
        return this.isJackpot;
    }

    private GroupItemType getGroupItemTypeNew() {
        this.groupItemType = GroupItemType.FAIL;
        this.prizeAmount = 0L;
        if (this.isThreeRed()) {
            this.groupItemType = GroupItemType.THREE_RED;
            this.prizeAmount = 10L;
            if (this.mutil == 100) {
                this.isJackpot = true;
            }
        } else if (this.isThreeGreen()) {
            this.groupItemType = GroupItemType.THREE_GREEN;
            this.prizeAmount = 1L;
        } else if (this.isThreeWhite()) {
            this.groupItemType = GroupItemType.THREE_WHITE;
            this.prizeAmount = 0L;
        } else if (this.isThreeBlue()) {
            this.groupItemType = GroupItemType.THREE_BLUE;
            this.prizeAmount = 0L;
        } else if (this.isThreeViolet()) {
            this.groupItemType = GroupItemType.THREE_VIOLET;
            this.prizeAmount = 0L;
        }
        return this.groupItemType;
    }

    private GroupItemType getGroupItemType() {
        this.groupItemType = GroupItemType.FAIL;
        this.prizeAmount = 0L;
        if (this.isThreeYellow()) {
            this.groupItemType = GroupItemType.THREE_YELLOW;
            this.isJackpot = true;
        } else if (this.isThreeViolet()) {
            this.groupItemType = GroupItemType.THREE_VIOLET;
            this.prizeAmount = 85L;
        } else if (this.isThreeBlue()) {
            this.groupItemType = GroupItemType.THREE_BLUE;
            this.prizeAmount = 40L;
        } else if (this.isThreeWhite()) {
            this.groupItemType = GroupItemType.THREE_BLUE;
            this.prizeAmount = 20L;
        } else if (this.isThreeGreen()) {
            this.groupItemType = GroupItemType.THREE_BLUE;
            this.prizeAmount = 8L;
        } else if (this.isThreeRed()) {
            this.groupItemType = GroupItemType.THREE_BLUE;
            this.prizeAmount = 3L;
        } else if (this.isDoubleKind()) {
            this.groupItemType = GroupItemType.DOUBLE_KIND;
            this.prizeAmount = 0L;
        } else if (this.isTwoKind()) {
            int check = this.getKindOfTwoKind();
            if (check == 1) {
                this.prizeAmount = 0L;
            } else if (check == 2) {
                this.prizeAmount = 0L;
            }
        }
        return this.groupItemType;
    }

    private void log(String msg) {
        System.out.println("Diamond<<GroupItem>>" + msg);
    }

    private boolean isThreeYellow() {
        return this.items[0] == this.items[1] && this.items[1] == this.items[2] && this.items[0] == 6;
    }

    private boolean isThreeViolet() {
        return this.isThreeOfAKind() && this.hasItem(5);
    }

    private boolean isThreeBlue() {
        return this.isThreeOfAKind() && this.hasItem(4);
    }

    private boolean isThreeWhite() {
        return this.isThreeOfAKind() && this.hasItem(3);
    }

    private boolean isThreeRed() {
        return this.isThreeOfAKindRed() && this.hasItem(1);
    }

    private boolean isThreeGreen() {
        return this.isThreeOfAKind() && this.hasItem(2);
    }

    private boolean hasItem(int itemValue) {
        return this.items[0] == itemValue || this.items[1] == itemValue || this.items[2] == itemValue;
    }

    private boolean isThreeOfAKindRed() {
        boolean check = false;
        if (this.items[0] == this.items[1] && this.items[1] == this.items[2]) {
            check = true;
        }
        return check;
    }

    private boolean isThreeOfAKind() {
        boolean check = false;
        if (this.items[0] == this.items[1] && this.items[1] == this.items[2]) {
            check = true;
        } else if (this.items[0] == this.items[1] && this.items[2] == 6) {
            check = true;
        } else if (this.items[0] == this.items[2] && this.items[1] == 6) {
            check = true;
        } else if (this.items[1] == this.items[2] && this.items[0] == 6) {
            check = true;
        } else if (this.items[1] == this.items[2] && this.items[1] == 6 && this.items[0] != 6) {
            check = true;
        } else if (this.items[0] == this.items[2] && this.items[0] == 6 && this.items[1] != 6) {
            check = true;
        } else if (this.items[0] == this.items[1] && this.items[0] == 6 && this.items[2] != 6) {
            check = true;
        }
        return check;
    }

    private boolean isDoubleKind() {
        return this.hasItem(1) && this.hasItem(2) && this.hasItem(6);
    }

    private boolean isTwoKind() {
        return this.items[0] == this.items[1] && (this.items[0] == 1 || this.items[0] == 2) || this.items[0] == this.items[2] && (this.items[0] == 1 || this.items[0] == 2) || this.items[1] == this.items[2] && (this.items[1] == 1 || this.items[1] == 2);
    }

    private int getKindOfTwoKind() {
        int check = 0;
        if (this.items[0] == this.items[1] && this.items[0] == 1 || this.items[0] == this.items[2] && this.items[0] == 1 || this.items[1] == this.items[2] && this.items[1] == 1) {
            check = 1;
        } else if (this.items[0] == this.items[1] && this.items[0] == 2 || this.items[0] == this.items[2] && this.items[0] == 2 || this.items[1] == this.items[2] && this.items[1] == 2) {
            check = 2;
        }
        return check;
    }
}

