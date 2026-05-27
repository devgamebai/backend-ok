/*
 * Decompiled with CFR 0.144.
 * 
 * Could not load the following classes:
 *  bitzero.server.entities.User
 *  com.vinplay.cardlib.models.Card
 *  com.vinplay.cardlib.models.Deck
 */
package game.modules.minigame.entities;

import bitzero.server.entities.User;
import com.vinplay.cardlib.models.Card;
import com.vinplay.cardlib.models.Deck;
import java.util.List;

public class CaoThapInfo {
    private int id;
    private User user;
    private long referenceId;
    private Deck deck;
    private Card card;
    private short step;
    private short time;
    private long moneyUp;
    private long money;
    private long moneyDown;
    private byte numA;
    private List<Card> carryCards;
    /** [SUN-589/Phase4c] RTP Engine: force-lose flag set tại startPlay, persist qua các bước tiếp theo */
    private boolean forceLose;

    public CaoThapInfo() {
    }

    public CaoThapInfo(User user, long referenceId, Deck deck, Card card, short step, short time, long money, byte numA, List<Card> carryCards, long moneyUp, long moneyDown, int id) {
        this.user = user;
        this.referenceId = referenceId;
        this.money = money;
        this.deck = deck;
        this.step = step;
        this.time = time;
        this.numA = numA;
        this.card = card;
        this.carryCards = carryCards;
        this.moneyUp = moneyUp;
        this.moneyDown = moneyDown;
        this.id = id;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public List<Card> getCarryCards() {
        return this.carryCards;
    }

    public void setCarryCards(List<Card> carryCards) {
        this.carryCards = carryCards;
    }

    public User getUser() {
        return this.user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public short getStep() {
        return this.step;
    }

    public void setStep(short step) {
        this.step = step;
    }

    public short getTime() {
        return this.time;
    }

    public void setTime(short time) {
        this.time = time;
    }

    public Deck getDeck() {
        return this.deck;
    }

    public void setDeck(Deck deck) {
        this.deck = deck;
    }

    public long getMoney() {
        return this.money;
    }

    public void setMoney(long money) {
        this.money = money;
    }

    public long getMoneyUp() {
        return this.moneyUp;
    }

    public void setMoneyUp(long moneyUp) {
        this.moneyUp = moneyUp;
    }

    public long getMoneyDown() {
        return this.moneyDown;
    }

    public void setMoneyDown(long moneyDown) {
        this.moneyDown = moneyDown;
    }

    public byte getNumA() {
        return this.numA;
    }

    public void setNumA(byte numA) {
        this.numA = numA;
    }

    public long getReferenceId() {
        return this.referenceId;
    }

    public void setReferenceId(long referenceId) {
        this.referenceId = referenceId;
    }

    public Card getCard() {
        return this.card;
    }

    public void setCard(Card card) {
        this.card = card;
    }

    public boolean isForceLose() {
        return this.forceLose;
    }

    public void setForceLose(boolean forceLose) {
        this.forceLose = forceLose;
    }
}

