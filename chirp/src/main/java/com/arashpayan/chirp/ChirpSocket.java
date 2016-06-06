package com.arashpayan.chirp;

import android.support.annotation.CheckResult;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
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
        mSocket.joinGroup(mGroupAddress);

        mReadBuf = new byte[Chirp.MAX_MSG_LENGTH];
        mReadPacket = new DatagramPacket(mReadBuf, Chirp.MAX_MSG_LENGTH);
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
            return null;
        }
//        String str = new String(buf, 0, packet.getLength(), "utf-8");
//        logi("msgstr: " + str);
        ByteArrayInputStream bais = new ByteArrayInputStream(mReadBuf, 0, mReadPacket.getLength());
        Message msg;
        try {
            msg = Chirp.sGson.fromJson(new InputStreamReader(bais), Message.class);
            msg.setAddress(mReadPacket.getAddress());
        } catch (Throwable t) {
            logw("bad message received", t);
            return null;
        }

        if (!msg.isValid()) {
            logi("returning null because message isn't valid");
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
    }

    protected void send(byte[] bytes) throws IOException {
        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, mGroupAddress, CHIRP_PORT);
        mSocket.send(packet);
    }
}