package tk.munditv.controller;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import org.jxmpp.stringprep.XmppStringprepException;

import tk.munditv.controller.xmpp.Logger;
import tk.munditv.controller.xmpp.MessageCallback;
import tk.munditv.controller.xmpp.XmppAccount;
import tk.munditv.controller.xmpp.XmppServiceBroadcastEventEmitter;
import tk.munditv.controller.xmpp.XmppServiceBroadcastEventReceiver;
import tk.munditv.controller.xmpp.XmppServiceCommand;
import tk.munditv.controller.xmpp.database.SqLiteDatabase;
import tk.munditv.controller.xmpp.database.providers.MessagesProvider;

public class ControllerApp extends Application implements MessageCallback {

    private static final String TAG = "ControllerApp";
    public static final String DATABASE_NAME = "messages.db";

    private static ControllerApp mInstance ;
    private static String remoteAccount;
    private static String serialnumber;
    private static String username;
    private static String password;
    private static String hostname;
    private static String servicename;
    private static boolean isBOSH;
    private static boolean isSync;
    private XmppAccount mXmppAccount;
    private XmppServiceBroadcastEventReceiver receiver;
    private MessagesProvider messagesProvider;
    private SqLiteDatabase mDatabase;

    public static synchronized ControllerApp getInstance () {
        return mInstance ;
    }

    public void initialize() {
        receiver = new XmppServiceBroadcastEventReceiver();
        receiver.register(this);
        XmppServiceBroadcastEventEmitter.initialize(this, "tk.munditv.controller");
        receiver.setMessageCallback(this);
        mDatabase = new SqLiteDatabase(this, DATABASE_NAME);
        messagesProvider = new MessagesProvider(mDatabase);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        initialize();
        RefreshPreference();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    public void RefreshPreference() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        username = preferences.getString("username", null);
        password = preferences.getString("password", null);
        hostname = preferences.getString("xmppserver", "webrtc01.mundi-tv.tk");
        servicename = hostname;
        isBOSH = preferences.getString("boshflag", "BOSH").equals("BOSH");
        isSync = preferences.getBoolean("sync", true);
        Logger.info(TAG, "Shared Preferences data");
        Logger.info(TAG, " username = " + username);
        Logger.info(TAG, " password = " + password);
        Logger.info(TAG, " hostname = " + hostname);
        Logger.info(TAG, " isBOSH = " + isBOSH);
        Logger.info(TAG, " isSync = " + isSync);
        mXmppAccount = new XmppAccount();
        mXmppAccount.setBOSHFlag(isBOSH);
        mXmppAccount.setHost(hostname);
        mXmppAccount.setHttps(true);
        mXmppAccount.setPort(443);
        mXmppAccount.setPassword(password);
        mXmppAccount.setServiceName(servicename);
        mXmppAccount.setXmppJid(username);
        mXmppAccount.setResourceName("MundiController");
        mXmppAccount.setPersonalMessage("Working!");
    }

    public String getAccount() {
        Logger.debug(TAG, "getAccount() "  + username);
        return username;
    }

    public String getRemoteAccount() {
        Logger.debug(TAG, "getRemoteAccount() " + remoteAccount );
        return remoteAccount;
    }

    public void doConnect() {
        XmppServiceCommand.connect(this, mXmppAccount);
    }

    public void doLogin() {
        XmppServiceCommand.login(this, mXmppAccount);
    }

    public void doRegister() {
        XmppServiceCommand.register(this, mXmppAccount);
    }

    public void setRemoteAccount(String account, String hostname) {
        Logger.debug(TAG, "setRemoteAccount()");
        remoteAccount = account + "@" + hostname;
    }

    public void setSerialNumber(String serialno) {
        serialnumber = serialno;
    }

    public void sendMessage(String message) {
        if (remoteAccount == null) {
            remoteAccount = "admin@webrtc01.mundi-tv.tk";
        }
        XmppServiceCommand.sendMessage(this, remoteAccount, message);
    }

    @Override
    public void onMessageAdded(String remoteAccount, boolean incoming)
            throws XmppStringprepException {

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
    public void onAuthenticated() {
        Logger.debug(TAG, "onAuthenticated()");

    }

    @Override
    public void onAuthenticateFailure() {
        Logger.debug(TAG, "onAuthenticateFailure()");

    }

    @Override
    public void onRegistered() {
        Logger.debug(TAG, "onRegistered()");

    }

    @Override
    public void onRegisterFailure() {
        Logger.debug(TAG, "onRegisterFailure()");

    }

}
