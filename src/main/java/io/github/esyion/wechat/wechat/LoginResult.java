package io.github.esyion.wechat.wechat;

/**
 * QR login result.
 */
public class LoginResult {
    public boolean connected;
    public String botToken;
    public String accountId;
    public String baseUrl;
    public String userId;
    public Boolean alreadyConnected;
    public String message;

    public static LoginResult success(String botToken, String accountId, String baseUrl, String userId) {
        LoginResult result = new LoginResult();
        result.connected = true;
        result.botToken = botToken;
        result.accountId = accountId;
        result.baseUrl = baseUrl;
        result.userId = userId;
        result.message = "Login confirmed.";
        return result;
    }

    public static LoginResult failure(String message) {
        LoginResult result = new LoginResult();
        result.connected = false;
        result.message = message;
        return result;
    }
}
