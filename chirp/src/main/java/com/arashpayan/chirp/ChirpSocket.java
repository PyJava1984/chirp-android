package com.arashpayan.chirp;

import android.support.annotation.CheckResult;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Map;

import static com.arashpayan.chirp.ChirpLog.logi;
import static com.arashpayan.chirp.ChirpLog.logw;

/**
 * Created by Arash Payan (https://arashpayan.com) on 6/3/16.
 */
public class ChirpSocket {

    private static final String IPv4_GROUP = "224.0.0.224";
    private static final String IPv6_GROUP = "[FF06::224]";
    private static final int CHIRP_PORT = 6464;
    private static final int MAX_MSG_LENGTH = 33 * 1024;

    private InetAddress mGroupAddress;
    private MulticastSocket mSocket;
    private final byte[] mReadBuf;
    private final DatagramPacket mReadPacket;

    protected ChirpSocket(boolean ip6) throws IOException {
        if (ip6) {
            mGroupAddress = InetAddress.getByName(IPv6_GROUP);
        } else {
            mGroupAddress = InetAddress.getByName(IPv4_GROUP);
        }
        mSocket = new MulticastSocket(CHIRP_PORT);
        mSocket.setReuseAddress(true);
        // join the group on every interface we have
        InetSocketAddress sockAddr = new InetSocketAddress(mGroupAddress, CHIRP_PORT);
        Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
        int numJoins = 0;
        while (ifaces.hasMoreElements()) {
            NetworkInterface ifc = ifaces.nextElement();
            if (ifc.supportsMulticast() && !ifc.isVirtual()) {
                try {
                    mSocket.joinGroup(sockAddr, ifc);
                    numJoins++;
                } catch (SocketException ignore) {}
            }
        }
        if (numJoins == 0) {
            logi("failed to join any interfaces");
            // TODO: https://github.com/arashpayan/chirp-android/issues/2
        }

        mReadBuf = new byte[MAX_MSG_LENGTH];
        mReadPacket = new DatagramPacket(mReadBuf, MAX_MSG_LENGTH);
    }

    protected void close() {
        mSocket.close();
    }

    @CheckResult
    protected Message read() {
        try {
            mSocket.receive(mReadPacket);
        } catch (IOException e) {
            // hacky, but gets the job done for debugging purposes only
            if (!e.getMessage().contains("Socket closed")) {
                logw("listen IOException", e);
            }
            return null;
        }
        if (mReadPacket.getLength() == 0) {
            if (Chirp.Debug) {
                logi("read: received 0 length packet");
            }
            return null;
        }
        if (Chirp.Debug) {
            String str = null;
            try {
                str = new String(mReadBuf, 0, mReadPacket.getLength(), "utf-8");
                logi(Thread.currentThread().getName() +  " read: " + str);
            } catch (UnsupportedEncodingException e) {
                logi("failed to convert message to string: " + e.getMessage());
            }
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(mReadBuf, 0, mReadPacket.getLength());
        Message msg;
        try {
            msg = Chirp.sGson.fromJson(new InputStreamReader(bais), Message.class);
            msg.setAddress(mReadPacket.getAddress());
        } catch (Throwable t) {
            logw("bad message received", t);
            return null;
        }

        ChirpError err = msg.isValid();
        if (err != null) {
            logi("returning null because message isn't valid: " + err);
            return null;
        }

        return msg;
    }

    protected void send(Message msg) throws IOException {
        Map<String, Object> map = msg.toMap();
        String json = Chirp.sGson.toJson(map);
        byte[] bytes = json.getBytes();
        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, mGroupAddress, CHIRP_PORT);
        mSocket.send(packet);
        try {Thread.sleep(20); } catch (InterruptedException ie) {logi("interrupted sleep");};
        mSocket.send(packet);
    }

    protected void send(byte[] bytes) throws IOException {
        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, mGroupAddress, CHIRP_PORT);
        mSocket.send(packet);
        try {Thread.sleep(20); } catch (InterruptedException ie) {logi("interrupted sleep");};
        mSocket.send(packet);
    }
}
