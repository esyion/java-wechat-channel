package com.esyion.wechat.error;

/**
 * ilink server returned non-zero ret/errcode, or HTTP transport failure.
 */
public class WechatApiError extends RuntimeException {
    private final int ret;
    private final Integer errcode;
    private final String errmsg;

    public WechatApiError(int ret, Integer errcode, String errmsg) {
        super(buildMessage(ret, errcode, errmsg));
        this.ret = ret;
        this.errcode = errcode;
        this.errmsg = errmsg;
    }

    public WechatApiError(int ret, Integer errcode, String errmsg, Throwable cause) {
        super(buildMessage(ret, errcode, errmsg), cause);
        this.ret = ret;
        this.errcode = errcode;
        this.errmsg = errmsg;
    }

    public int getRet() {
        return ret;
    }

    public Integer getErrcode() {
        return errcode;
    }

    public String getErrmsg() {
        return errmsg;
    }

    private static String buildMessage(int ret, Integer errcode, String errmsg) {
        StringBuilder sb = new StringBuilder("WechatApiError");
        sb.append(": ret=").append(ret);
        if (errcode != null) {
            sb.append(", errcode=").append(errcode);
        }
        if (errmsg != null && !errmsg.isEmpty()) {
            sb.append(", errmsg=").append(errmsg);
        }
        return sb.toString();
    }

    /** Session expired errcode */
    public static final int SESSION_EXPIRED = -14;
}
