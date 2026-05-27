/*
 * Decompiled with CFR 0.144.
 */
package game.caro.server.logic;

import game.caro.server.logic.CaroCell;
import game.caro.server.logic.CountResult;
import game.caro.server.logic.GameResult;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CaroTable {
    public static final int WIDTH = 15;
    public static final int HEIGHT = 15;
    public static final int MAX_COUNT = 225;
    public static final int O_TICK = 2;
    public static final int X_TICK = 1;
    public static final int BLANK = 0;
    public static final int UP = 0;
    public static final int DOWN = 1;
    public static final int RIGHT = 2;
    public static final int LEFT = 3;
    public static final int UP_LEFT = 4;
    public static final int UP_RIGHT = 5;
    public static final int DOWN_LEFT = 6;
    public static final int DOWN_RIGHT = 7;
    public static final int NUMBER_TO_WIN = 5;
    public int[][] map = new int[15][15];
    public int[] dx = new int[]{0, 0, 1, -1, -1, 1, -1, 1};
    public int[] dy = new int[]{1, -1, 0, 0, 1, 1, -1, -1};
    public int count = 0;
    public byte lastX = (byte)-1;
    public byte lastY = (byte)-1;

    public CaroTable() {
        this.reset();
    }

    public static void main(String[] args) {
        CaroTable table = new CaroTable();
        GameResult win = null;
        win = table.play(1, 1, 2);
        win = table.play(2, 1, 1);
        win = table.play(1, 2, 2);
        win = table.play(2, 2, 1);
        win = table.play(1, 3, 2);
        win = table.play(2, 3, 1);
        win = table.play(1, 4, 2);
        win = table.play(2, 4, 1);
        win = table.play(1, 5, 2);
        System.out.println((Object)win.result);
    }

    public void reset() {
        this.count = 0;
        this.lastX = (byte)-1;
        this.lastY = (byte)-1;
        for (int i = 0; i < 15; ++i) {
            for (int j = 0; j < 15; ++j) {
                this.map[i][j] = 0;
            }
        }
    }

    public GameResult play(int x, int y, int value) {
        if (this.tick(x, y, value)) {
            int check = this.checkWin(x, y);
            if (check != 0) {
                return this.winLost(x, y, check);
            }
            if (this.count == 225) {
                return this.result(GameResult.Name.DRAW);
            }
            return this.result(GameResult.Name.CONTINUE);
        }
        return this.result(GameResult.Name.ERROR);
    }

    public GameResult winLost(int x, int y, int direct) {
        GameResult res = new GameResult();
        res.result = GameResult.Name.WIN_LOST;
        this.findWinLostPosition(res, x, y, direct);
        return res;
    }

    public void findWinLostPosition(GameResult res, int x, int y, int direct) {
        if (direct == 1) {
            this.findWinLostPosition(res, x, y, 0, 1);
        }
        if (direct == 2) {
            this.findWinLostPosition(res, x, y, 3, 2);
        }
        if (direct == 3) {
            this.findWinLostPosition(res, x, y, 4, 7);
        }
        if (direct == 4) {
            this.findWinLostPosition(res, x, y, 5, 6);
        }
    }

    public void findWinLostPosition(GameResult res, int x, int y, int d1, int d2) {
        res.listX = new ArrayList<Byte>();
        res.listY = new ArrayList<Byte>();
        int tick = this.map[x][y];
        int a = x;
        int b = y;
        res.listX.add((byte)a);
        res.listY.add((byte)b);
        while (this.inBoard(a += this.dx[d1], b += this.dy[d1]) && this.map[a][b] == tick) {
            res.listX.add((byte)a);
            res.listY.add((byte)b);
        }
        while (this.inBoard(a += this.dx[d2], b += this.dy[d2]) && this.map[a][b] == tick) {
            res.listX.add((byte)a);
            res.listY.add((byte)b);
        }
    }

    public GameResult result(GameResult.Name result) {
        GameResult res = new GameResult();
        res.result = result;
        return res;
    }

    public boolean tick(int x, int y, int value) {
        if (!this.inBoard(x, y) || value != 2 && value != 1) {
            return false;
        }
        if (this.map[x][y] == 2 || this.map[x][y] == 1) {
            return false;
        }
        this.map[x][y] = value;
        ++this.count;
        this.lastX = (byte)x;
        this.lastY = (byte)y;
        return true;
    }

    public CountResult count(int x, int y, int direct) {
        CountResult result = new CountResult();
        int a = x;
        int b = y;
        int tick = this.map[a][b];
        while (this.inBoard(a += this.dx[direct], b += this.dy[direct])) {
            int t = this.map[a][b];
            if (t != tick) {
                if (t != 2 && t != 1) break;
                result.block = true;
                break;
            }
            ++result.value;
        }
        return result;
    }

    public int checkWin(int x, int y) {
        CountResult c6;
        CountResult c8;
        CountResult c4;
        CountResult c2;
        CountResult c1 = this.count(x, y, 0);
        if (this.checkWin(c1, c2 = this.count(x, y, 1))) {
            return 1;
        }
        CountResult c3 = this.count(x, y, 3);
        if (this.checkWin(c3, c4 = this.count(x, y, 2))) {
            return 2;
        }
        CountResult c5 = this.count(x, y, 4);
        if (this.checkWin(c5, c6 = this.count(x, y, 7))) {
            return 3;
        }
        CountResult c7 = this.count(x, y, 5);
        if (this.checkWin(c7, c8 = this.count(x, y, 6))) {
            return 4;
        }
        return 0;
    }

    public boolean checkWin(CountResult c1, CountResult c2) {
        int total = c1.value + c2.value + 1;
        if (total >= 5) {
            return true;
        }
        if (total == 5) {
            return !c1.block || !c2.block;
        }
        return false;
    }

    public boolean inBoard(int x, int y) {
        return x >= 0 && x < 15 && y < 15 && y >= 0;
    }

    public CaroCell findFreePosition() {
        Random rd = new Random();
        int remain = 225 - this.count;
        int x = Math.abs(rd.nextInt(remain) + 1);
        CaroCell c = new CaroCell();
        int count = 0;
        for (int i = 0; i < 15; ++i) {
            for (int j = 0; j < 15; ++j) {
                if (this.map[i][j] != 0 || ++count != x) continue;
                c.x = i;
                c.y = j;
                return c;
            }
        }
        return c;
    }

    public byte[][] getMap() {
        byte[][] newMap = new byte[15][15];
        for (int i = 0; i < 15; ++i) {
            for (int j = 0; j < 15; ++j) {
                newMap[i][j] = (byte)this.map[i][j];
            }
        }
        return newMap;
    }
}

