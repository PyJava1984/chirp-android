package com.arashpayan.chirp;

import android.support.annotation.NonNull;

/**
 * Created by Arash Payan (https://arashpayan.com) on 6/2/16.
 */
public interface ChirpBrowserListener {

    /**
     * Called when a published service has been discovered. The service object is guaranteed to
     * have at least one IP address associated with it (IPv4 or IPv6) at this point. If the
     * publisher is running on a host with IPv4 and IPv6, then a follow up call to
     * <code>onServiceUpdated</code> will include the second IP address.
     * @param service the discovered service
     */
    void onServiceDiscovered(@NonNull Service service);

    /**
     * Called when a new IP address has been discovered for the service. This is usually called
     * right after <code>onServiceDiscovered</code> in the case of services on hosts that have
     * IPv4 and IPv6 addresses. <code>service</code> will have the newly discovered IP address.
     * Less frequently, this will also get called when the IP address of the host publishing the
     * service changes.
     * @param service the updated Service
     */
    void onServiceUpdated(@NonNull Service service);

    /**
     * Called when a service is no longer being published on the local network. This occurs when
     * the publisher has explicitly stopped publishing the service, or if the service has simply
     * expired (maybe because the publisher was abruptly powered down or disconnected from the
     * network.
     * @param service the service that was removed
     */
    void onServiceRemoved(@NonNull Service service);

}
