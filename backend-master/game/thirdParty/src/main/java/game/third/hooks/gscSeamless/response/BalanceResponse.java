package game.third.hooks.gscSeamless.response;

import com.google.gson.Gson;

import java.util.List;

public class BalanceResponse {
    private int code;
    private String message;
    private List<BalanceResponseItem> data;

    public BalanceResponse(List<BalanceResponseItem> data) {
        this.code = 0;
        this.message = "";
        this.data = data;
    }

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public List<BalanceResponseItem> getData() { return data; }
    public void setData(List<BalanceResponseItem> data) { this.data = data; }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}