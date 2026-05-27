/*
 * Decompiled with CFR 0.152.
 */
package game.third.hooks.gscSeamless.response;

public class WithdrawResponse {
    private int code;
    private String message;
    private double beforeBalance;
    private double balance;

    public WithdrawResponse(int code, String message, double beforeBalance, double balance) {
        this.code = code;
        this.message = message;
        this.beforeBalance = beforeBalance;
        this.balance = balance;
    }

    public int getCode() {
        return this.code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public double getBeforeBalance() {
        return this.beforeBalance;
    }

    public void setBeforeBalance(double beforeBalance) {
        this.beforeBalance = beforeBalance;
    }

    public double getBalance() {
        return this.balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public String toString() {
        return "WithdrawResponse{code=" + this.code + ", message='" + this.message + '\'' + ", beforeBalance=" + this.beforeBalance + ", balance=" + this.balance + '}';
    }

    public String toJson() {
        java.text.DecimalFormat df = new java.text.DecimalFormat("0.####");
        df.setDecimalSeparatorAlwaysShown(false);
        java.text.DecimalFormatSymbols sym = new java.text.DecimalFormatSymbols();
        sym.setDecimalSeparator('.');
        df.setDecimalFormatSymbols(sym);
        String bb = df.format(this.beforeBalance);
        String b = df.format(this.balance);
        if (this.code != 0) {
            return "{\"code\":" + this.code + ",\"message\":\"" + this.message + "\",\"data\":null}";
        }
        return "{\"code\":" + this.code + ",\"message\":\"" + this.message + "\",\"data\":[{\"before_balance\":" + bb + ",\"balance\":" + b + "}]}";
    }
}

