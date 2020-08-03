package tk.munditv.xmpptest;

import android.app.Application;

import tk.munditv.xmppservice.Logger;
import tk.munditv.xmppservice.XmppAccount;
import tk.munditv.xmppservice.XmppServiceBroadcastEventEmitter;
import tk.munditv.xmppservice.XmppServiceBroadcastEventReceiver;
import tk.munditv.xmppservice.XmppServiceCallback;
import tk.munditv.xmppservice.XmppServiceCommand;

public class MainApp extends Application implements XmppServiceCallback {
    private static final String TAG = "MainApp";

    private static MainApp mInstance;
    private static XmppServiceBroadcastEventReceiver receiver;

    public static synchronized MainApp getInstance() {
        return mInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Logger.debug(TAG, "onCreate()");
        mInstance = this;
        receiver = new XmppServiceBroadcastEventReceiver();
        receiver.register(this);
        receiver.setCallback(this);

        XmppServiceBroadcastEventEmitter.initialize(this, "tk.munditv.xmppservice");
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        Logger.debug(TAG, "onTerminate()");
        receiver.unregister(this);
        receiver = null;
    }

    public void connect(XmppAccount account) {
        Logger.debug(TAG, "connect()");
        XmppServiceCommand.connect(this, account);
    }

    public void disconnect() {
        Logger.debug(TAG, "disconnect()");
        XmppServiceCommand.disconnect(this);
    }


    @Override
    public void onConnected() {
        Logger.debug(TAG, "onConnected()");
    }

    @Override
    public void onDisconnected() {
        Logger.debug(TAG, "onDisconnected()");
    }

    @Override
    public void onRosterChanged() {
        Logger.debug(TAG, "onRosterChanged()");
    }

    @Override
    public void onMessageAdded(String remoteAccount, boolean incoming) {
        Logger.debug(TAG, "onMessageAdded()");
    }

    @Override
    public void onContactAdded(String remoteAccount) {
        Logger.debug(TAG, "onContactAdded()");
    }

    @Override
    public void onContactRemoved(String remoteAccount) {
        Logger.debug(TAG, "onContactRemoved()");
    }

    @Override
    public void onContactRenamed(String remoteAccount, String newAlias) {
        Logger.debug(TAG, "onContactRenamed()");
    }

    @Override
    public void onContactAddError(String remoteAccount) {
        Logger.debug(TAG, "onContactAddError()");
    }

    @Override
    public void onConversationsCleared(String remoteAccount) {
        Logger.debug(TAG, "onConversationsCleared()");
    }

    @Override
    public void onConversationsClearError(String remoteAccount) {
        Logger.debug(TAG, "onConversationsClearError()");
    }

    @Override
    public void onMessageSent(long messageId) {
        Logger.debug(TAG, "onMessageSent()");
    }

    @Override
    public void onMessageDeleted(long messageId) {
        Logger.debug(TAG, "onMessageDeleted()");
    }
}
