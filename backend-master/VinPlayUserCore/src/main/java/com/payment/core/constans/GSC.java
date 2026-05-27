/*
 * Decompiled with CFR 0.152.
 */
package com.payment.core.constans;

public class GSC {

    public static enum CurrencyCode {
        CNY("CNY", 1),
        USD("USD", 1),
        KRW("KRW", 1),
        MYR("MYR", 1),
        SGD("SGD", 1),
        JPY("JPY", 1),
        THB("THB", 1),
        IDR("IDR", 1),
        VND("VND", 1),
        AUD("AUD", 1),
        GBP("GBP", 1),
        CHF("CHF", 1),
        MXN("MXN", 1),
        CAD("CAD", 1),
        RUB("RUB", 1),
        INR("INR", 1),
        RON("RON", 1),
        DKK("DKK", 1),
        NOK("NOK", 1),
        COP("COP", 1),
        MMK("MMK", 1),
        PLN("PLN", 1),
        HRK("HRK", 1),
        CZK("CZK", 1),
        HUF("HUF", 1),
        ZAR("ZAR", 1),
        SEK("SEK", 1),
        NZD("NZD", 1),
        TRY("TRY", 1),
        BND("BND", 1),
        KHR("KHR", 1),
        USDT("USDT", 1),
        BDT("BDT", 1),
        EUR("EUR", 1),
        BRL("BRL", 1),
        PHP("PHP", 1),
        TND("TND", 1),
        TWD("TWD", 1),
        UAH("UAH", 1),
        PKR("PKR", 1),
        HKD("HKD", 1),
        MAD("MAD", 1),
        EGP("EGP", 1),
        ZMW("ZMW", 1),
        NPR("NPR", 1),
        KSH("KSH", 1),
        AED("AED", 1),
        LAK("LAK", 1),
        NGN("NGN", 1),
        KES("KES", 1),
        UGX("UGX", 1),
        SAR("SAR", 1),
        AZN("AZN", 1),
        BGN("BGN", 1),
        ARS("ARS", 1),
        AMD("AMD", 1),
        NTD("NTD", 1),
        CLP("CLP", 1),
        LKR("LKR", 1),
        VES("VES", 1),
        PEN("PEN", 1),
        MNT("MNT", 1),
        IDR2("IDR2", 1000),
        KRW2("KRW2", 1000),
        MMK2("MMK2", 1000),
        VND2("VND2", 1000),
        LAK2("LAK2", 1000),
        KHR2("KHR2", 1000);

        private final String name;
        private final int exchangeRate;

        private CurrencyCode(String name, int exchangeRate) {
            this.name = name;
            this.exchangeRate = exchangeRate;
        }

        public int getExchangeRate() {
            return this.exchangeRate;
        }

        public String getName() {
            return this.name;
        }

        public static CurrencyCode findByName(String name) {
            for (CurrencyCode currency : CurrencyCode.values()) {
                if (!currency.getName().equalsIgnoreCase(name)) continue;
                return currency;
            }
            return null;
        }
    }

    public static enum LanguageCode {
        ENGLISH(0, "English"),
        TRADITIONAL_CHINESE(1, "Traditional Chinese"),
        SIMPLIFIED_CHINESE(2, "Simplified Chinese"),
        THAI(3, "Thai"),
        INDONESIAN(4, "Indonesian"),
        JAPANESE(5, "Japanese"),
        KOREAN(6, "Korean"),
        VIETNAMESE(7, "Vietnamese"),
        DEUTSCH(8, "Deutsch"),
        ESPANOL(9, "Espanol"),
        FRANCAIS(10, "Francais"),
        RUSSIAN(11, "Russian"),
        PORTUGUESE(12, "Portuguese"),
        BURMESE(13, "Burmese"),
        DANISH(14, "Danish"),
        FINNISH(15, "Finnish"),
        ITALIAN(16, "Italian"),
        DUTCH(17, "Dutch"),
        NORWEGIAN(18, "Norwegian"),
        POLISH(19, "Polish"),
        ROMANIAN(20, "Romanian"),
        SWEDISH(21, "Swedish"),
        TURKISH(22, "Turkish"),
        BULGARIAN(23, "Bulgarian"),
        CZECH(24, "Czech"),
        GREEK(25, "Greek"),
        HUNGARIAN(26, "Hungarian"),
        BRAZILIAN_PORTUGUESE(27, "Brazilian Portuguese"),
        SLOVAK(28, "Slovak"),
        GEORGIAN(29, "Georgian"),
        LATVIAN(30, "Latvian"),
        UKRAINIAN(31, "Ukrainian"),
        ESTONIAN(32, "Estonian"),
        FILIPINO(33, "Filipino"),
        CAMBODIAN(34, "Cambodian"),
        LAO(35, "Lao"),
        MALAY(36, "Malay"),
        CANTONESE(37, "Cantonese"),
        TAMIL(38, "Tamil"),
        HINDI(39, "Hindi"),
        EUROPEAN_SPANISH(40, "European Spanish"),
        AZERBAIJANI(41, "Azerbaijani"),
        BRUNEI_DARUSSALAM(42, "Brunei Darussalam"),
        CROATIAN(43, "Croatian");

        private final int code;
        private final String description;

        private LanguageCode(int code, String description) {
            this.code = code;
            this.description = description;
        }

        public int getCode() {
            return this.code;
        }

        public String getDescription() {
            return this.description;
        }
    }
}

