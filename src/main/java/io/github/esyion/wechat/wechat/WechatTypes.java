package io.github.esyion.wechat.wechat;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * WeChat ilink protocol types.
 */
public class WechatTypes {

    public static class BaseInfo {
        @JsonProperty("channel_version")
        public String channelVersion;
        @JsonProperty("bot_agent")
        public String botAgent;
    }

    public static class CdnMedia {
        @JsonProperty("encrypt_query_param")
        public String encryptQueryParam;
        @JsonProperty("aes_key")
        public String aesKey;
        @JsonProperty("encrypt_type")
        public Integer encryptType;
        @JsonProperty("full_url")
        public String fullUrl;
    }

    public static class ImageItem {
        public CdnMedia media;
        @JsonProperty("thumb_media")
        public CdnMedia thumbMedia;
        public String aeskey;
        public String url;
        @JsonProperty("mid_size")
        public Integer midSize;
        @JsonProperty("thumb_size")
        public Integer thumbSize;
        @JsonProperty("thumb_height")
        public Integer thumbHeight;
        @JsonProperty("thumb_width")
        public Integer thumbWidth;
        @JsonProperty("hd_size")
        public Integer hdSize;
    }

    public static class VoiceItem {
        public CdnMedia media;
        @JsonProperty("encode_type")
        public Integer encodeType;
        @JsonProperty("bits_per_sample")
        public Integer bitsPerSample;
        @JsonProperty("sample_rate")
        public Integer sampleRate;
        public Integer playtime;
        public String text;
    }

    public static class FileItem {
        public CdnMedia media;
        @JsonProperty("file_name")
        public String fileName;
        public String md5;
        public String len;
    }

    public static class VideoItem {
        public CdnMedia media;
        @JsonProperty("video_size")
        public Integer videoSize;
        @JsonProperty("play_length")
        public Integer playLength;
        @JsonProperty("video_md5")
        public String videoMd5;
        @JsonProperty("thumb_media")
        public CdnMedia thumbMedia;
        @JsonProperty("thumb_size")
        public Integer thumbSize;
        @JsonProperty("thumb_height")
        public Integer thumbHeight;
        @JsonProperty("thumb_width")
        public Integer thumbWidth;
    }

    public static class TextItem {
        public String text;
    }

    public static class RefMessage {
        @JsonProperty("message_item")
        public MessageItem messageItem;
        public String title;
    }

    public static class MessageItem {
        public Integer type;
        @JsonProperty("create_time_ms")
        public Long createTimeMs;
        @JsonProperty("update_time_ms")
        public Long updateTimeMs;
        @JsonProperty("is_completed")
        public Boolean isCompleted;
        @JsonProperty("msg_id")
        public String msgId;
        @JsonProperty("ref_msg")
        public RefMessage refMsg;
        @JsonProperty("text_item")
        public TextItem textItem;
        @JsonProperty("image_item")
        public ImageItem imageItem;
        @JsonProperty("voice_item")
        public VoiceItem voiceItem;
        @JsonProperty("file_item")
        public FileItem fileItem;
        @JsonProperty("video_item")
        public VideoItem videoItem;
    }

    public static class WeixinMessage {
        public Integer seq;
        @JsonProperty("message_id")
        public Long messageId;
        @JsonProperty("from_user_id")
        public String fromUserId;
        @JsonProperty("to_user_id")
        public String toUserId;
        @JsonProperty("client_id")
        public String clientId;
        @JsonProperty("create_time_ms")
        public Long createTimeMs;
        @JsonProperty("update_time_ms")
        public Long updateTimeMs;
        @JsonProperty("delete_time_ms")
        public Long deleteTimeMs;
        @JsonProperty("session_id")
        public String sessionId;
        @JsonProperty("group_id")
        public String groupId;
        @JsonProperty("message_type")
        public Integer messageType;
        @JsonProperty("message_state")
        public Integer messageState;
        @JsonProperty("item_list")
        public List<MessageItem> itemList;
        @JsonProperty("context_token")
        public String contextToken;
    }

    public static class GetUpdatesReq {
        @JsonProperty("get_updates_buf")
        public String getUpdatesBuf;
        @JsonProperty("base_info")
        public BaseInfo baseInfo;
    }

    public static class GetUpdatesResp {
        public Integer ret;
        public Integer errcode;
        public String errmsg;
        public List<WeixinMessage> msgs;
        @JsonProperty("get_updates_buf")
        public String getUpdatesBuf;
        @JsonProperty("longpolling_timeout_ms")
        public Long longpollingTimeoutMs;
    }

    public static class SendMessageReq {
        public Msg msg;

        public static class Msg {
            @JsonProperty("from_user_id")
            public String fromUserId;
            @JsonProperty("to_user_id")
            public String toUserId;
            @JsonProperty("client_id")
            public String clientId;
            @JsonProperty("message_type")
            public Integer messageType;
            @JsonProperty("message_state")
            public Integer messageState;
            @JsonProperty("item_list")
            public List<MessageItem> itemList;
            @JsonProperty("context_token")
            public String contextToken;
        }
    }

    public static class GetUploadUrlReq {
        public String filekey;
        @JsonProperty("media_type")
        public Integer mediaType;
        @JsonProperty("to_user_id")
        public String toUserId;
        @JsonProperty("rawsize")
        public Long rawsize;
        @JsonProperty("rawfilemd5")
        public String rawfilemd5;
        @JsonProperty("filesize")
        public Long filesize;
        @JsonProperty("thumb_rawsize")
        public Long thumbRawsize;
        @JsonProperty("thumb_rawfilemd5")
        public String thumbRawfilemd5;
        @JsonProperty("thumb_filesize")
        public Long thumbFilesize;
        @JsonProperty("no_need_thumb")
        public Boolean noNeedThumb;
        public String aeskey;
        @JsonProperty("base_info")
        public BaseInfo baseInfo;
    }

    public static class GetUploadUrlResp {
        @JsonProperty("upload_param")
        public String uploadParam;
        @JsonProperty("thumb_upload_param")
        public String thumbUploadParam;
        @JsonProperty("upload_full_url")
        public String uploadFullUrl;
    }

    public static class UploadedFileInfo {
        public String fileKey;
        public String downloadEncryptedQueryParam;
        public String aeskey;
        public long fileSize;
        public long fileSizeCiphertext;
    }

    public static class SendTypingReq {
        @JsonProperty("ilink_user_id")
        public String ilinkUserId;
        @JsonProperty("typing_ticket")
        public String typingTicket;
        public Integer status;
        @JsonProperty("base_info")
        public BaseInfo baseInfo;
    }

    public static class GetConfigReq {
        @JsonProperty("ilink_user_id")
        public String ilinkUserId;
        @JsonProperty("context_token")
        public String contextToken;
        @JsonProperty("base_info")
        public BaseInfo baseInfo;
    }

    public static class GetConfigResp {
        public Integer ret;
        public String errmsg;
        @JsonProperty("typing_ticket")
        public String typingTicket;
    }

    public static class NotifyResp {
        public Integer ret;
        public String errmsg;
    }

    public static class GetBotQrcodeResp {
        public String qrcode;
        @JsonProperty("qrcode_img_content")
        public String qrcodeImgContent;
    }

    public static class GetQrcodeStatusResp {
        public String status;
        @JsonProperty("bot_token")
        public String botToken;
        @JsonProperty("ilink_bot_id")
        public String ilinkBotId;
        @JsonProperty("baseurl")
        public String baseUrl;
        @JsonProperty("ilink_user_id")
        public String ilinkUserId;
        @JsonProperty("redirect_host")
        public String redirectHost;
    }

    // Constants
    public static final int UPLOAD_MEDIA_TYPE_IMAGE = 1;
    public static final int UPLOAD_MEDIA_TYPE_VIDEO = 2;
    public static final int UPLOAD_MEDIA_TYPE_FILE = 3;
    public static final int UPLOAD_MEDIA_TYPE_VOICE = 4;

    public static final int MESSAGE_TYPE_USER = 1;
    public static final int MESSAGE_TYPE_BOT = 2;

    public static final int MESSAGE_STATE_NEW = 0;
    public static final int MESSAGE_STATE_GENERATING = 1;
    public static final int MESSAGE_STATE_FINISH = 2;

    public static final int MESSAGE_ITEM_TYPE_TEXT = 1;
    public static final int MESSAGE_ITEM_TYPE_IMAGE = 2;
    public static final int MESSAGE_ITEM_TYPE_VOICE = 3;
    public static final int MESSAGE_ITEM_TYPE_FILE = 4;
    public static final int MESSAGE_ITEM_TYPE_VIDEO = 5;

    public static final int TYPING_STATUS_TYPING = 1;
    public static final int TYPING_STATUS_CANCEL = 2;
}
