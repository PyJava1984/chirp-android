package com.arashpayan.chirp;

import android.support.annotation.StringDef;
import android.text.TextUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Arash Payan (https://arashpayan.com) on 6/1/16.
 */
class Message {

    @StringDef({MESSAGE_TYPE_NEW_LISTENER, MESSAGE_TYPE_PUBLISH, MESSAGE_TYPE_REMOVE_SERVICE, QUEUE_POISON_PILL})
    @Retention(RetentionPolicy.SOURCE)
    protected @interface MessageType {}
    protected static final String MESSAGE_TYPE_NEW_LISTENER = "new_listener";
    protected static final String MESSAGE_TYPE_PUBLISH = "publish";
    protected static final String MESSAGE_TYPE_REMOVE_SERVICE = "remove_service";
    protected static final String QUEUE_POISON_PILL = "poison_pill";

    protected String ipAddress;
    protected byte[] publisherPayload;

    @MessageType
    protected String type;
    protected String senderId;
    protected String serviceName;
    protected Map<String, Object> payload;
    protected int ttl;

    protected boolean isIP6() {
        if (ipAddress == null) {
            throw new RuntimeException("There's no IP address on this message");
        }
        return ipAddress.contains(":");
    }

    protected boolean isValid() {
        switch (type) {
            case MESSAGE_TYPE_NEW_LISTENER:
            case MESSAGE_TYPE_PUBLISH:
            case MESSAGE_TYPE_REMOVE_SERVICE:
                break;
            default:
                return false;
        }

        //noinspection SimplifiableIfStatement
        if (TextUtils.isEmpty(senderId)) {
            return false;
        }

        return Chirp.isValidServiceName(serviceName);
    }

    protected void setAddress(InetAddress address) {
        if (address == null) {
            ipAddress = null;
            return;
        }

        String hostAddr = address.getHostAddress();
        if (hostAddr.contains("%")) {
            ipAddress = hostAddr.split("%")[0];
        } else {
            ipAddress = hostAddr;
        }
    }

    protected Map<String, Object> toMap() {
        HashMap<String, Object> json = new HashMap<>();
        json.put("type", type);
        switch (type) {
            case MESSAGE_TYPE_NEW_LISTENER:
                json.put("sender_id", senderId);
                break;
            default:
                break;
        }

        return json;
    }

    @Override
    public String toString() {
        return "Message{" +
                "ipAddress='" + ipAddress + '\'' +
                ", publisherPayload=" + Arrays.toString(publisherPayload) +
                ", type=" + type +
                ", senderId='" + senderId + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", payload=" + payload +
                ", ttl=" + ttl +
                '}';
    }

}
