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
 * A <code>ChirpPublisher</code> is used to publish a Chirp service on the local network. You can
 * construct and start the publisher manually, or use the simpler class builder, like so:
 * <code>
 *     ChirpPublisher publisher = Chirp.publish("com.example.service").
 *                                      start(getApplication())
 * </code>
 *
 * Optionally, you can also set a payload for the publisher that gets sent along to clients
 * listening for your service. The payload can contain any data you want that can be serialized
 * into a JSON object, as long as it's less than <code>Chirp.MAX_PAYLOAD_BYTES</code> (32KB) in size
 * (after serialization). For example, if you're using to mDNS/Bonjour, you might want to include a
 * port number for your service in the payload:
 * <code>
 *     Map<String, Object> payload = new HashMap<>();
 *     payload.put("port", 1337);
 *     ChirpPublisher publisher Chirp.publish("com.example.service").
 *                                    payload(payload).
 *                                    start(getApplication());
 * </code>
 *
 * When you no longer want your service published:
 * <code>
 *     publisher.stop();
 * </code>
 */
public class ChirpPublisher {

    private final String mId;
    private final String mServiceName;
    private Map<String, Object> mPayload;
    private int mTtl;

    private volatile boolean mIsStarted;
    private WifiManager.MulticastLock mMulticastLock;
    private ExecutorService mExecutor;

    protected class Command {
        String type;
        Message message;
    }

    public static class Builder {

        private ChirpPublisher mPublisher;

        public Builder(@NonNull String serviceName) {
            mPublisher = new ChirpPublisher(serviceName);
        }

        /**
         * Sets a payload with arbitrary data to be associated with the service
         * @param p the payload
         * @return the <code>Builder</code> object for method chaining
         */
        @SuppressWarnings("unused")
        public Builder payload(Map<String, Object> p) {
            mPublisher.setPayload(p);
            return this;
        }

        /**
         * Sets the TTL of the service. There's not really a good reason to set this, unless you're
         * debugging the library or implementing Chirp in another language and need to test with this.
         * @param ttl the ttl to use for the service
         * @return the same <code>Builder</code> object for method chaining
         */
        @SuppressWarnings("unused")
        public Builder ttl(int ttl) {
            mPublisher.setTtl(ttl);
            return this;
        }

        /**
         * Starts the publisher and returns it
         * @param app the <code>Application</code> object is used instead of a <code>Context</code>
         *            to make sure an <code>Activity</code>, which could leak, isn't passed in
         * @return the newly created and started <code>ChirpPublisher</code>
         */
        public ChirpPublisher start(Application app) {
            mPublisher.start(app);
            return mPublisher;
        }

    }

    /**
     * Create a publisher that can publish the specified service name. It's always easier to use
     * <code>Chirp.publish(String)</code> to create, configure and start a <code>ChirpPublisher</code>
     * instead of using this constructor.
     * @param serviceName a valid service name
     */
    public ChirpPublisher(@NonNull String serviceName) {
        if (!Chirp.isValidServiceName(serviceName)) {
            throw new IllegalArgumentException("Invalid service name");
        }

        mServiceName = serviceName;
        mId = Chirp.getRandomId();
        mTtl = 60;
    }

    /**
     * Sets the payload for this service. If the payload is too large (> Chirp.MAX_PAYLOAD_BYTES
     * after serialization into JSON), then an <code>IllegalArgumentException</code> will be thrown.
     * This method has no effect if called after the publisher has been started.
     * @param p the payload
     */
    public void setPayload(Map<String, Object> p) {
        if (mIsStarted) {
            return;
        }

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

    /**
     * Sets the ttl of the service in seconds. Must be >= 10.
     * @param ttl
     */
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

    /**
     * Starts the publisher.
     * @param app the <code>Application</code> object is requested instead of a <code>Context</code>
     *            to avoid memory leaks from an <code>Activity</code> being passed in
     */
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

    /**
     * Stops the publisher. A message is sent before closing network connections to inform any
     * Chirp listeners that our service is no longer around, so they don't have to wait for the TTL
     * to expire before removing our service.
     *
     * The <code>ChirpPublisher</code> can not be started again after it has been stopped.
     */
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
