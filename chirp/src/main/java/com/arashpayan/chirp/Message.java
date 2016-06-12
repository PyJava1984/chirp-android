package com.arashpayan.chirp;

import android.support.annotation.StringDef;
import android.text.TextUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.InetAddress;
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

    protected ChirpError isValid() {
        // validate the sender id (should be 16 bytes, encoded as hexadecimal)
        if (!Chirp.isValidSenderId(senderId)) {
            return new ChirpError("invalid 'sender_id'");
        }
        if (TextUtils.isEmpty(serviceName)) {
            return new ChirpError("'service_name' is missing");
        }

        switch (type) {
            case MESSAGE_TYPE_NEW_LISTENER:
                // wildcard is acceptable for listeners
                if (!serviceName.equals("*")) {
                    if (!Chirp.isValidServiceName(serviceName)) {
                        return new ChirpError("invalid 'service_name");
                    }
                }
                break;
            case MESSAGE_TYPE_PUBLISH:
                if (!Chirp.isValidServiceName(serviceName)) {
                    return new ChirpError("invalid 'service_name'");
                }
                if (ttl < 10) {
                    return new ChirpError("invalid 'ttl'");
                }
                break;
            case MESSAGE_TYPE_REMOVE_SERVICE:
                if (!Chirp.isValidServiceName(serviceName)) {
                    return new ChirpError("invalid 'service_name'");
                }
                break;
            default:
                // unknown message type
                return new ChirpError("unknown message type");
        }

        return null;
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
        json.put("sender_id", senderId);
        json.put("service_name", serviceName);
        switch (type) {
            case MESSAGE_TYPE_NEW_LISTENER:
                break;
            case MESSAGE_TYPE_PUBLISH:
                json.put("service_name", serviceName);
                json.put("ttl", ttl);
                if (payload != null) {
                    json.put("payload", payload);
                }
                break;
            case MESSAGE_TYPE_REMOVE_SERVICE:
                json.put("service_name", serviceName);
            default:
                break;
        }

        return json;
    }

    @Override
    public String toString() {
        return "Message{" +
                "ipAddress='" + ipAddress + '\'' +
                ", type=" + type +
                ", senderId='" + senderId + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", payload=" + payload +
                ", ttl=" + ttl +
                '}';
    }

}
