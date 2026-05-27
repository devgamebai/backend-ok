package com.vinplay.usercore.utils;

public class TelegramConfigList {
    private TelegramConfig login;
    private TelegramConfig util;
    private TelegramConfig warningCheat;
    private TelegramConfig pushNotificationDeposit;
    private TelegramConfig pushNotificationLode;

    public TelegramConfigList() {}

    public TelegramConfig getLogin() { return login; }
    public void setLogin(TelegramConfig login) { this.login = login; }
    public TelegramConfig getUtil() { return util; }
    public void setUtil(TelegramConfig util) { this.util = util; }
    public TelegramConfig getWarningCheat() { return warningCheat; }
    public void setWarningCheat(TelegramConfig warningCheat) { this.warningCheat = warningCheat; }
    public TelegramConfig getPushNotificationDeposit() { return pushNotificationDeposit; }
    public void setPushNotificationDeposit(TelegramConfig pushNotificationDeposit) { this.pushNotificationDeposit = pushNotificationDeposit; }
    public TelegramConfig getPushNotificationLode() { return pushNotificationLode; }
    public void setPushNotificationLode(TelegramConfig pushNotificationLode) { this.pushNotificationLode = pushNotificationLode; }
}
