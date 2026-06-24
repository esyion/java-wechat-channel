package io.github.esyion.wechat.channel;

import io.github.esyion.wechat.wechat.WechatTypes;

import java.util.List;

/**
 * Inbound message passed to onMessage handlers.
 */
public class ChannelMsg {
    public final String fromUserId;
    public final String contextToken;
    public final String text;
    public final List<MediaRef> media;
    public final WechatTypes.WeixinMessage raw;

    public ChannelMsg(String fromUserId, String contextToken, String text,
                     List<MediaRef> media, WechatTypes.WeixinMessage raw) {
        this.fromUserId = fromUserId;
        this.contextToken = contextToken;
        this.text = text;
        this.media = media;
        this.raw = raw;
    }
}
