package com.arashpayan.chirp;

import android.support.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

/**
 * An object that represents a Chirp service on the network.
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

    /**
     * Returns the IPv4 address of the host running the service.
     * @return the IPv4 address, or <code>null</code> if an IPv4 address has not been discovered yet
     */
    public String getIpv4() {
        return v4Ip;
    }

    /**
     * Returns the IPv6 address of the host running the service.
     * @return the IPv6 address, or <code>null</code> if an IPv6 address has not been discovered yet
     */
    public String getIpv6() {
        return v6Ip;
    }

    /**
     * @return the name of the service
     */
    public String getName() {
        return name;
    }

    /**
     * @return the payload of the service. <code>null</code> if the service does not have a payload
     */
    public Map<String, Object> getPayload() {
        return new HashMap<>(payload);
    }

    /**
     * @return the unique identifier for the publisher responsible for publishing this service.
     */
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Service service = (Service) o;

        return publisherId.equals(service.publisherId);

    }

    @Override
    public int hashCode() {
        return publisherId.hashCode();
    }
}
