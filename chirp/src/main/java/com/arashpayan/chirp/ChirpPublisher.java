package com.arashpayan.chirp;

import android.app.Application;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import static com.arashpayan.chirp.ChirpLog.logi;
import static com.arashpayan.chirp.ChirpLog.logw;

/**
 * Created by Arash Payan (https://arashpayan.com) on 6/3/16.
 */
public class ChirpPublisher {

    private final String mId;
    private final String mServiceName;
    private Map<String, Object> mPayload;
    private int mTtl;

    private volatile boolean mIsStarted;
    private WifiManager.MulticastLock mMulticastLock;
    private ExecutorService mExecutor;

    public class Command {
        String type;
        Message message;
    }

    public static class Builder {

        private ChirpPublisher mPublisher;

        public Builder(@NonNull String serviceName) {
            mPublisher = new ChirpPublisher(serviceName);
        }

        @SuppressWarnings("unused")
        public Builder payload(Map<String, Object> p) {
            mPublisher.setPayload(p);
            return this;
        }

        @SuppressWarnings("unused")
        public Builder ttl(int ttl) {
            mPublisher.setTtl(ttl);
            return this;
        }

        public ChirpPublisher start(Application app) {
            mPublisher.start(app);
            return mPublisher;
        }

    }

    public ChirpPublisher(@NonNull String serviceName) {
        if (!Chirp.isValidServiceName(serviceName)) {
            throw new IllegalArgumentException("Invalid service name");
        }

        mServiceName = serviceName;
        mId = Chirp.getRandomId();
        mTtl = 60;
    }

    public void setPayload(Map<String, Object> p) {
        if (p != null) {
            // we gotta check its size when serialized
            String json = Chirp.sGson.toJson(p);
            byte[] bytes = json.getBytes();
            if (bytes.length > Chirp.MAX_PAYLOAD_BYTES) {
                throw new IllegalArgumentException("Payload is too large. Max: " + Chirp.MAX_PAYLOAD_BYTES + " Serialized payload bytes: " + bytes.length);
            }
        }

        mPayload = p;
    }

    public void setTtl(@IntRange(from=10) int ttl) {
        if (ttl < 10) {
            throw new IllegalArgumentException("TTL must be at least 10 seconds");
        }

        mTtl = ttl;
    }

    private void serve(final ChirpSocket socket) {
        try {
//            logi("serve initial announce");
            Message announceMsg = new Message();
            announceMsg.type = Message.MESSAGE_TYPE_PUBLISH;
            announceMsg.senderId = mId;
            announceMsg.serviceName = mServiceName;
            announceMsg.payload = mPayload;
            announceMsg.ttl = mTtl;
            Map<String, Object> map = announceMsg.toMap();
            String json = Chirp.sGson.toJson(map);
            byte[] jsonBytes = json.getBytes();
            try {
                socket.send(jsonBytes);
            } catch (Throwable t) {
                logw("error sending initial announce", t);
            }

            final LinkedBlockingQueue<Command> commandQueue = new LinkedBlockingQueue<>();

            // start a thread that tells us to periodically broadcast
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    Command c = new Command();
                    c.type = "announce";
                    while (mIsStarted) {
                        try {
                            Thread.sleep((mTtl - 4) * 1000);
                        } catch (Throwable t) {
                            // we're being shutdown
//                            logi("announce thread is finishing");
                            return;
                        }
                        commandQueue.offer(c);
                    }
                }
            });
            // start a thread that just reads from the socket and packages the messages into a command
            mExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    while (mIsStarted) {
                        Message msg = socket.read();
                        if (msg == null) {
                            continue;
                        }
                        if (msg.senderId.equals(mId)) {
                            continue;
                        }
                        Command c = new Command();
                        c.type = "message";
                        c.message = msg;
                        commandQueue.offer(c);
                    }
//                    logi("reading thread is finishing");
                }
            });

            while (mIsStarted) {
                Command c = commandQueue.take();
                switch (c.type) {
                    case "announce":
//                        logi("announce command");
                        try {
                            socket.send(jsonBytes);
                        } catch (IOException e) {
                            logw("error sending response to new listener", e);
                        }
                        break;
                    case "message":
//                        logi("message command");
                        if (c.message.type.equals(Message.MESSAGE_TYPE_NEW_LISTENER)) {
                            try {
                                socket.send(jsonBytes);
                            } catch (IOException e) {
                                logw("error sending response to new listener", e);
                            }
                        }
                        break;
                }
            }
        } catch (InterruptedException e) {
//            logw("interrupted during command take", e);
            Message goodbye = new Message();
            goodbye.type = Message.MESSAGE_TYPE_REMOVE_SERVICE;
            goodbye.senderId = mId;
            goodbye.serviceName = mServiceName;
            try {
                socket.send(goodbye);
                Thread.sleep(50);
            } catch (IOException ex) {
                logw("problem sending goodbye", ex);
            } catch (InterruptedException iex) {
                logw("interrupted while waiting for goodbye to send", iex);
            }
        } finally {
//            logi("closing socket");
            socket.close();
        }
//        logi("serve thread is finishing");
    }

    public void start(Application app) {
        if (mIsStarted) {
            return;
        }

        WifiManager wifiMgr = (WifiManager) app.getSystemService(Context.WIFI_SERVICE);
        mMulticastLock = wifiMgr.createMulticastLock("Chirp Multicast Lock");
        mMulticastLock.setReferenceCounted(false);
        mMulticastLock.acquire();

        mExecutor = Executors.newCachedThreadPool();
        mIsStarted = true;

        // IPv4 socket
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ChirpSocket socket4 = new ChirpSocket(false);
                    serve(socket4);
                } catch (Throwable t) {
                    logw("error serving4", t);
                }
            }
        });
        // IPv6 socket
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ChirpSocket socket6 = new ChirpSocket(true);
                    serve(socket6);
                } catch (Throwable t) {
                    logw("error serving6", t);
                }
            }
        });
    }

    public void stop() {
        if (!mIsStarted) {
            return;
        }

        mIsStarted = false;
        // all the threads should clean up once they get interrupted by the shutdown
        mExecutor.shutdownNow();

        // The executor won't take new jobs now, and we don't want to block the thread calling this
        // method (its the user's thread and it might be main), but we need to wait 50ms for
        // removal messages to be sent before we release the multicast lock. So we'll do this the old
        // fashioned way.
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
//                    logw("MulticastLock releaser was interrupted", e);
                }
                mMulticastLock.release();
                mMulticastLock = null;
            }
        }, "MulticastLock releaser");
        t.start();
    }

}
