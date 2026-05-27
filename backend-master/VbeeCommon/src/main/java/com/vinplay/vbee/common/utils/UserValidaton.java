/*
 * Decompiled with CFR 0.144.
 */
package com.vinplay.vbee.common.utils;

import com.vinplay.vbee.common.config.VBeePath;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

public class UserValidaton {
    private static String USERNAME_PATTERN = "^[a-zA-Z0-9]{6,16}$";
    private static String NICKNAME_PATTERN = "^[a-zA-Z0-9_]{6,16}$";
    private static String PASSWORD_PATTERN = "^[a-zA-Z0-9@#$%^&*]{6,16}$";
    private static String EMAIL_PATTERN = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
    private static String MOBILE_PATTERN = "^[0-9]{10,15}$";
    private static String MOBILE_VN_PATTERN = "[0][0-9]{9,14}$";
    private static String IDENTIFICATION_PATTERN = "^[0-9]{9,12}$";
    private static String SERIAL_PIN_PATTERN = "^[a-zA-Z0-9]{5,20}$";
    private static String SERIAL_VIN_CARD_PATTERN = "^[0-9]{12,12}$";
    private static String PIN_VIN_CARD_PATTERN = "^[0-9]{14,14}$";
    private static String SERIAL_MEGA_CARD_PATTERN = "^[A-Z]{2,2}[0-9]{10,10}$";
    private static String PIN_MEGA_CARD_PATTERN = "^[0-9]{12,12}$";
    private static int ADDRESS_LENGHT = 100;
    private static List<String> NICKNAME_CONTAIN_INVALID = new ArrayList<String>();
    private static List<String> NICKNAME_START_INVALID = new ArrayList<String>();

    public static void init() throws IOException {
        Properties prop = new Properties();
        FileInputStream input = new FileInputStream(VBeePath.basePath.concat("config/validation.properties"));
        prop.load(input);
        USERNAME_PATTERN = prop.getProperty("USERNAME_PATTERN");
        NICKNAME_PATTERN = prop.getProperty("NICKNAME_PATTERN");
        PASSWORD_PATTERN = prop.getProperty("PASSWORD_PATTERN");
        EMAIL_PATTERN = prop.getProperty("EMAIL_PATTERN");
        MOBILE_PATTERN = prop.getProperty("MOBILE_PATTERN");
        MOBILE_VN_PATTERN = prop.getProperty("MOBILE_VN_PATTERN");
        IDENTIFICATION_PATTERN = prop.getProperty("IDENTIFICATION_PATTERN");
        ADDRESS_LENGHT = Integer.parseInt(prop.getProperty("ADDRESS_LENGHT"));
        NICKNAME_CONTAIN_INVALID = Arrays.asList(prop.getProperty("NICKNAME_CONTAIN_INVALID").split(","));
        NICKNAME_START_INVALID = Arrays.asList(prop.getProperty("NICKNAME_START_INVALID").split(","));
    }

    public static boolean validateSerialMegaCard(String serial) {
        return serial != null && !serial.isEmpty() && Pattern.compile(SERIAL_MEGA_CARD_PATTERN).matcher(serial).matches();
    }

    public static boolean validatePinMegaCard(String pin) {
        return pin != null && !pin.isEmpty() && Pattern.compile(PIN_MEGA_CARD_PATTERN).matcher(pin).matches();
    }

    public static boolean validateUserName(String username) {
        return username != null && !username.isEmpty() && Pattern.compile(USERNAME_PATTERN).matcher(username).matches();
    }

    public static boolean validatePassword(String password) {
        return password != null && !password.isEmpty() && Pattern.compile(PASSWORD_PATTERN).matcher(password).matches();
    }

    public static boolean validateMobile(String mobile) {
        return mobile != null && !mobile.isEmpty() && Pattern.compile(MOBILE_PATTERN).matcher(mobile).matches();
    }

    public static boolean validateSerialPin(String value) {
        return value != null && !value.isEmpty() && Pattern.compile(SERIAL_PIN_PATTERN).matcher(value).matches();
    }

    public static boolean validatePinVinCard(String value) {
        return value != null && !value.isEmpty() && Pattern.compile(PIN_VIN_CARD_PATTERN).matcher(value).matches();
    }

    public static boolean validateSerialVinCard(String value) {
        return value != null && Pattern.compile(SERIAL_VIN_CARD_PATTERN).matcher(value).matches();
    }

    public static boolean validateMobileVN(String mobile) {
        return mobile != null && !mobile.isEmpty() && Pattern.compile(MOBILE_VN_PATTERN).matcher(mobile).matches();
    }

    public static boolean validateEmail(String email) {
        return email != null && !email.isEmpty() && Pattern.compile(EMAIL_PATTERN).matcher(email).matches() && email.length() < 100;
    }

    public static boolean validateAddress(String address) {
        return address != null && !address.isEmpty() && address.length() <= ADDRESS_LENGHT;
    }

    public static boolean validateNicknameSpecial(String nickname) {
        if (nickname != null && !nickname.isEmpty()) {
            nickname = nickname.toLowerCase();
            for (String entry : NICKNAME_CONTAIN_INVALID) {
                if (!nickname.contains(entry)) continue;
                return false;
            }
            for (String entry : NICKNAME_START_INVALID) {
                if (nickname.length() < entry.length() || !nickname.substring(0, entry.length()).equals(entry)) continue;
                return false;
            }
        } else {
            return false;
        }
        return true;
    }

    public static boolean validateNickname(String nickname) {
        return nickname != null && !nickname.isEmpty() && Pattern.compile(NICKNAME_PATTERN).matcher(nickname).matches();
    }

    public static boolean validate(String value, String pattern) {
        return value != null && !value.isEmpty() && Pattern.compile(pattern).matcher(value).matches();
    }

    public static boolean validateIdentification(String identification) {
        return identification != null && !identification.isEmpty() && (identification.length() == 9 || identification.length() == 12) && Pattern.compile(IDENTIFICATION_PATTERN).matcher(identification).matches();
    }
}

