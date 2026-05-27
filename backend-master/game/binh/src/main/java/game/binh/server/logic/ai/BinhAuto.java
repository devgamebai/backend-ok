/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  game.utils.GameUtils
 *  game.utils.LoggerUtils
 */
package game.binh.server.logic.ai;

import com.vinplay.vbee.common.config.VBeePath;
import game.binh.server.logic.ai.BinhSuit;
import game.utils.GameUtils;
import game.utils.LoggerUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class BinhAuto {
    private static BinhAuto ins = null;
    private List<BinhSuit> binhAt = new LinkedList<BinhSuit>();
    private List<BinhSuit> binhThuong = new LinkedList<BinhSuit>();
    private List<BinhSuit> jackpotAt = new LinkedList<BinhSuit>();
    private List<BinhSuit> jackpotThuong = new LinkedList<BinhSuit>();
    private Random rd = new Random();
    private volatile int lastDay = 0;
    private static String basePath = VBeePath.basePath;

    public static void main(String[] args) {
        long t = System.currentTimeMillis();
        BinhSuit suit = BinhAuto.instance().getSuit(0);
        long dt = System.currentTimeMillis() - t;
        System.out.println(dt);
        System.out.println(suit);
    }

    public static BinhAuto instance() {
        if (ins == null) {
            ins = new BinhAuto();
        }
        return ins;
    }

    private BinhAuto() {
        this.initJackpot();
        this.init();
    }

    public void loadData(int rule, int fileIndex, List<BinhSuit> list, List<BinhSuit> jackpotList) throws Exception {
        StringBuilder sb = new StringBuilder();
        // sb.append(System.getProperty("user.dir"));
        sb.append(basePath);
        if (rule == 0) {
            sb.append("/data/binhthuong/binh_newbie").append(fileIndex).append(".properties");
        } else {
            sb.append("/data/binhat/binh_advance").append(fileIndex).append(".properties");
        }
        File file = new File(sb.toString());
        FileInputStream fileStream = new FileInputStream(file);
        InputStreamReader decoder = new InputStreamReader((InputStream)fileStream, "UTF-8");
        BufferedReader reader = new BufferedReader(decoder);
        String text = null;
        int count = 0;
        Object suilt = null;
        String[] lines = new String[4];
        do {
            text = reader.readLine();
            int d = count % 5;
            if (d != 4) {
                lines[d] = text;
            } else {
                BinhSuit suit = new BinhSuit(lines);
                if (!suit.canJackpot()) {
                    list.add(suit);
                } else {
                    jackpotList.add(suit);
                }
            }
            ++count;
        } while (text != null);
        fileStream.close();
        ((Reader)decoder).close();
        reader.close();
    }

    public void initJackpot() {
        try {
            this.loadData(0, 200, this.binhThuong, this.jackpotThuong);
            this.loadData(1, 200, this.binhAt, this.jackpotAt);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public synchronized void init() {
        try {
            long t1 = System.currentTimeMillis();
            this.lastDay = Calendar.getInstance().get(6);
            int random = new Random().nextInt(200);
            this.binhThuong.clear();
            this.binhAt.clear();
            this.loadData(0, random, this.binhThuong, this.jackpotThuong);
            this.loadData(1, random, this.binhAt, this.jackpotAt);
            long t2 = System.currentTimeMillis();
            LoggerUtils.error((String)"binh", (Object[])new Object[]{"Binh Data loading time(ms):", t2 - t1, this.binhThuong.size(), this.jackpotThuong.size(), this.jackpotAt.size(), this.binhAt.size()});
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized BinhSuit getSuit(int rule, boolean canJackpot) {
        int today = Calendar.getInstance().get(6);
        if (today != this.lastDay) {
            this.init();
        }
        if (!canJackpot) {
            return this.getSuit(rule);
        }
        int rd = GameUtils.rd.nextInt(2000);
        if (rd == 0) {
            return this.getSuitJackpot(rule);
        }
        return this.getSuit(rule);
    }

    private BinhSuit getSuit(int rule) {
        int max = 0;
        int randomIndex = 0;
        try {
            max = this.binhThuong.size();
            if (rule == 1) {
                max = this.binhAt.size();
            }
            randomIndex = this.rd.nextInt(max);
            if (rule == 0) {
                return this.binhThuong.get(randomIndex);
            }
            return this.binhAt.get(randomIndex);
        }
        catch (Exception e) {
            LoggerUtils.error((String)"binh", (Object[])new Object[]{"ERROR BinhAuTo getSuit", "binhThuong =", this.binhThuong.size(), "jackpotThuong =", this.jackpotThuong.size(), "binhAt =", this.binhAt.size(), "jackpotAt =", this.jackpotAt.size(), "max =", max, "randomIndex =", randomIndex});
            return null;
        }
    }

    public synchronized BinhSuit getSuitJackpot(int rule) {
        int max = this.jackpotThuong.size();
        if (rule == 1) {
            max = this.jackpotAt.size();
        }
        int randomIndex = this.rd.nextInt(max);
        if (rule == 0) {
            return this.jackpotThuong.get(randomIndex);
        }
        return this.jackpotAt.get(randomIndex);
    }

    public class BinhDataLoad
    implements Runnable {
        @Override
        public void run() {
            try {
                com.vinplay.vbee.common.utils.GameHealthServer.tick();
                for (int i = 1; i < 200; ++i) {
                    BinhAuto.this.loadData(0, i, BinhAuto.this.binhThuong, BinhAuto.this.jackpotThuong);
                    BinhAuto.this.loadData(1, i, BinhAuto.this.binhAt, BinhAuto.this.jackpotAt);
                }
                LoggerUtils.debug((String)"binh", (Object[])new Object[]{"COMPLETED LOADING BINH DATA"});
            }
            catch (Throwable e) {
                LoggerUtils.error((String)"binh", (Object[])new Object[]{"ERROR BinhDataLoad run:", e.toString()});
            }
        }
    }

}

