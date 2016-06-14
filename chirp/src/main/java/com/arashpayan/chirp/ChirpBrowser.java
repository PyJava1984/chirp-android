package com.arashpayan.chirp;

import android.app.Application;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import static com.arashpayan.chirp.ChirpLog.logi;
import static com.arashpayan.chirp.ChirpLog.logw;

/**
 * Created by arash on 5/31/16.
 */
public class ChirpBrowser {

    private final String mId;
    private final String mServiceName;
    private ChirpBrowserListener mListener;
    private ExecutorService mExecutor;
    private final LinkedBlockingQueue<Message> mIncomingMessages;
    private final HashMap<String, Service> mKnownServices;
    private volatile boolean mIsStarted;
    private WifiManager.MulticastLock mMulticastLock;
    private ChirpSocket mSocket4;
    private ChirpSocket mSocket6;
    private Handler mListenerHandler;

    public static class Builder {

        private String mServiceName;
        private ChirpBrowserListener mListener;
        private Handler mHandler;

        public Builder(@NonNull String serviceName) {
            mServiceName = serviceName;
        }

        @SuppressWarnings("unused")
        public Builder listener(ChirpBrowserListener l) {
            mListener = l;
            return this;
        }

        @SuppressWarnings("unused")
        public Builder looper(Handler h) {
            mHandler = h;
            return this;
        }

        @SuppressWarnings("unused")
        public ChirpBrowser start(Application app) {
            ChirpBrowser cb = new ChirpBrowser(mServiceName);
            cb.setListener(mListener);
            cb.setHandler(mHandler);
            cb.start(app);
            return cb;
        }

    }

    public ChirpBrowser(@NonNull String serviceName) {
        if (!serviceName.equals("*") && !Chirp.isValidServiceName(serviceName)) {
            throw new IllegalArgumentException("Invalid service name");
        }
        mServiceName = serviceName;
        mId = Chirp.getRandomId();
        mIncomingMessages = new LinkedBlockingQueue<>();
        mKnownServices = new HashMap<>();
    }

    private void checkForExpirations() {
        long now = System.currentTimeMillis();
        LinkedList<Service> toRemove = new LinkedList<>();
        for (String pubId : mKnownServices.keySet()) {
            Service service = mKnownServices.get(pubId);
            if (service.expiration < now) {
                toRemove.add(service);
            }
        }

        for (Service service : toRemove) {
            mKnownServices.remove(service.publisherId);
            notifyServiceRemoved(service);
        }
    }

    private void handleMessages() throws InterruptedException {
        while (mIsStarted) {
            try {
                Message msg = mIncomingMessages.take();
                switch (msg.type) {
                    case Message.MESSAGE_TYPE_PUBLISH:
                        handlePublish(msg);
                        break;
                    case Message.MESSAGE_TYPE_REMOVE_SERVICE:
                        handleRemoval(msg);
                        break;
                    case Message.QUEUE_EXPIRATION_CHECK:
                        checkForExpirations();
                        break;
                }
            } catch (InterruptedException ignore) {
            } catch (Throwable t) {
                logw("problem taking from messages queue", t);
            }
        }
    }

    private void handlePublish(Message msg) {
        // is this a service we're interested in?
        if (!mServiceName.equals("*")) {
            if (!msg.serviceName.equals(mServiceName)) {
                return;
            }
        }

        Service service = mKnownServices.get(msg.senderId);
        long ttl = System.currentTimeMillis() + msg.ttl * 1000;
        if (service == null) {
            service = new Service(msg.senderId);
            service.expiration = ttl;
            if (msg.isIP6()) {
                service.v6Ip = msg.ipAddress;
                service.v6IpExpiration = ttl;
            } else {
                service.v4Ip = msg.ipAddress;
                service.v4IpExpiration = ttl;
            }
            service.name = msg.serviceName;
            service.payload = msg.payload;
            notifyServiceDiscovered(service);
            mKnownServices.put(service.publisherId, service);
        } else {
            service.expiration = ttl;
            boolean updatedIp = false;
            if (msg.isIP6()) {
                service.v6IpExpiration = ttl;
                if (service.v6Ip == null) {
                    service.v6Ip = msg.ipAddress;
                    updatedIp = true;
                } else {
                    if (!service.v6Ip.equals(msg.ipAddress)) {
                        service.v6Ip = msg.ipAddress;
                        updatedIp = true;
                    }
                }
            } else {
                service.v4IpExpiration = ttl;
                if (service.v4Ip == null) {
                    service.v4Ip = msg.ipAddress;
                    updatedIp = true;
                } else {
                    if (!service.v4Ip.equals(msg.ipAddress)) {
                        service.v4Ip = msg.ipAddress;
                        updatedIp = true;
                    }
                }
            }
            if (updatedIp) {
                notifyServiceUpdated(service);
            }
        }
    }

    private void handleRemoval(Message msg) {
        // is this a service we're interested in?
        if (!mServiceName.equals("*")) {
            if (!msg.serviceName.equals(mServiceName)) {
                return;
            }
        }

        // do we have a record for this service?
        Service service = mKnownServices.remove(msg.senderId);
        if (service == null) {
            return;
        }
        notifyServiceRemoved(service);
    }

    @SuppressWarnings("unused")
    public boolean isStarted() {
        return mIsStarted;
    }

    private void listen(ChirpSocket socket) throws UnsupportedEncodingException {
        Message helloMsg = new Message();
        helloMsg.type = Message.MESSAGE_TYPE_NEW_LISTENER;
        helloMsg.senderId = mId;
        helloMsg.serviceName = mServiceName;
        try {
            socket.send(helloMsg);
        } catch (Throwable t) {
            logw("failed to send hello message", t);
        }

        while (mIsStarted) {
            Message msg = socket.read();
            if (msg == null) {
                continue;
            }
            mIncomingMessages.offer(msg);
        }

        socket.close();
        logi("finishing listen");
    }

    private void notifyServiceDiscovered(@NonNull final Service service) {
        if (mListener != null) {
            mListenerHandler.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onServiceDiscovered(service);
                }
            });
        }
    }

    private void notifyServiceRemoved(@NonNull final Service service) {
        if (mListener != null) {
            mListenerHandler.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onServiceRemoved(service);
                }
            });
        }
    }

    private void notifyServiceUpdated(@NonNull final Service service) {
        if (mListener != null) {
            mListenerHandler.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onServiceUpdated(service);
                }
            });
        }
    }

    public void setListener(ChirpBrowserListener l) {
        mListener = l;
    }

    protected void setHandler(Handler h) {
        mListenerHandler = h;
    }

    public void start(@NonNull Application app) {
        if (mIsStarted) {
            return;
        }

        WifiManager wifiMgr = (WifiManager) app.getSystemService(Context.WIFI_SERVICE);
        mMulticastLock = wifiMgr.createMulticastLock("Chirp Multicast Lock");
        mMulticastLock.setReferenceCounted(false);
        mMulticastLock.acquire();

        mExecutor = Executors.newCachedThreadPool();
        mIsStarted = true;

        if (mListenerHandler == null) {
            mListenerHandler = new Handler(Looper.getMainLooper());
        }

        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    mSocket4 = new ChirpSocket(false);
                    listen(mSocket4);
                } catch (Throwable t) {
                    logw("error listening4", t);
                }
            }
        });
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    mSocket6 = new ChirpSocket(true);
                    listen(mSocket6);
                } catch (Throwable t) {
                    logw("error listening6", t);
                }
            }
        });
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    handleMessages();
                } catch (Throwable t) {
                    logw("error handling messages", t);
                }
            }
        });
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Message expCheck = new Message();
                    expCheck.type = Message.QUEUE_EXPIRATION_CHECK;
                    while (mIsStarted) {
                        try {
                            Thread.sleep(10000); // 10 seconds
                        } catch (InterruptedException ignore) {}
                        mIncomingMessages.offer(expCheck);
                    }
                } catch (Throwable t) {
                    logw("error generating expiration checks", t);
                }
            }
        });
    }

    public void stop() {
        if (!mIsStarted) {
            return;
        }

        mIsStarted = false;
        mExecutor.shutdownNow();
        // check if the sockets are null, in case there was a problem creating them
        if (mSocket4 != null) {
            mSocket4.close();
            mSocket4 = null;
        }
        if (mSocket6 != null) {
            mSocket6.close();
            mSocket6 = null;
        }
        mMulticastLock.release();
        mMulticastLock = null;
        mListenerHandler = null;
    }

}
