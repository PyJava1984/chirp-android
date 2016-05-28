package com.arashpayan.chirp;

import android.app.Application;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * Created by Arash Payan (https://arashpayan.com) on 5/14/16.
 */
public class Chirp {

    private static final String IPv4_GROUP = "224.0.0.224";
    private static final String IPv6_GROUP = "[FF06::224]";
    private static final int CHIRP_PORT = 6464;

    public static final int MAX_MSG_LENGTH = 64 * 1024;
    protected static final String TAG = "Chirp";
    private ScheduledExecutorService mExecutor;
    private WifiManager.MulticastLock mMulticastLock;

    public Chirp() {
//        mExecutor = Executors.newSingleThreadScheduledExecutor();
        mExecutor = Executors.newScheduledThreadPool(2);
        System.out.println("you created a chirper");
    }

    public void listen(final Application app) {
        // acquire the lock
        WifiManager wifiMgr = (WifiManager) app.getSystemService(Context.WIFI_SERVICE);
        mMulticastLock = wifiMgr.createMulticastLock("Chirp Multicast Lock");
        mMulticastLock.setReferenceCounted(false);
        mMulticastLock.acquire();

        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    _listen();
                } catch (Throwable t) {
                    Log.w(TAG, "run: ", t);
                }
            }
        });

        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    _listen6();
                } catch (Throwable t) {
                    Log.w(TAG, "run6: ", t);
                }
            }
        });
    }

    private void _listen() {
        Log.i(TAG, "_listen: ");
        try {
            InetAddress addr = InetAddress.getByName(IPv4_GROUP);
            MulticastSocket socket = new MulticastSocket(CHIRP_PORT);
            socket.joinGroup(addr);
            byte[] buf = new byte[MAX_MSG_LENGTH];
            DatagramPacket packet = new DatagramPacket(buf, MAX_MSG_LENGTH);
            while (true) {
                Log.i(TAG, "_listen: waiting on a packet");
                socket.receive(packet);
                if (packet.getLength() == 0) {
                    Log.i(TAG, "_listen: A 0 length packet?");
                    continue;
                }
                String msg = new String(buf, 0, packet.getLength(), "utf-8");
                Log.i(TAG, "msg: " + msg);
            }
        } catch (Throwable t) {
            Log.w(TAG, "_listen: ", t);
        }
    }

    private void _listen6() {
        Log.i(TAG, "_listen6: ");
        try {
            InetAddress addr = InetAddress.getByName(IPv6_GROUP);
            MulticastSocket socket = new MulticastSocket(CHIRP_PORT);
            socket.joinGroup(addr);
            byte[] buf = new byte[MAX_MSG_LENGTH];
            DatagramPacket packet = new DatagramPacket(buf, MAX_MSG_LENGTH);
            while (true) {
                Log.i(TAG, "_listen6: waiting on a packet");
                socket.receive(packet);
                if (packet.getLength() == 0) {
                    Log.i(TAG, "_listen6: A 0 length packet?");
                    continue;
                }
                String msg = new String(buf, 0, packet.getLength(), "utf-8");
                Log.i(TAG, "msg6: " + msg);
            }
        } catch (Throwable t) {
            Log.w(TAG, "_listen6: ", t);
        }
    }

}
