package com.arashpayan.chirp;

import android.support.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Arash Payan (https://arashpayan.com) on 6/1/16.
 */
public class Service {

    @NonNull
    protected final String publisherId;
    protected String v4Ip;
    protected long v4IpExpiration;
    protected String v6Ip;
    protected long v6IpExpiration;
    public String name;
    public Map<String, Object> payload;
    protected long expiration;

    protected Service(@NonNull  String pubId) {
        publisherId = pubId;
    }

    public String getIPv4() {
        return v4Ip;
    }

    public String getIPv6() {
        return v6Ip;
    }

    public String getName() {
        return name;
    }

    public Map<String, Object> getPayload() {
        return new HashMap<>(payload);
    }

    public String getPublisherId() {
        return publisherId;
    }

    @Override
    public String toString() {
        return "Service{" +
                "publisherId='" + publisherId + '\'' +
                ", v4Ip='" + v4Ip + '\'' +
                ", v6Ip='" + v6Ip + '\'' +
                ", name='" + name + '\'' +
                ", payload=" + payload +
                '}';
    }
}
