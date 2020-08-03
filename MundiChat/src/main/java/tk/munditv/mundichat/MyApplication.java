package tk.munditv.mundichat;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.RemoteException;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import org.jivesoftware.smackx.vcardtemp.packet.VCard;

import tk.munditv.mundichat.service.ChatService;
import tk.munditv.mundichat.utils.ActionStrings;
import tk.munditv.mundichat.utils.Logger;
import tk.munditv.mundichat.xmpp.XmppAccount;
import tk.munditv.mundichat.xmpp.XmppRosterEntry;
import tk.munditv.mundichat.xmpp.XmppServiceBroadcastEventEmitter;
import tk.munditv.mundichat.xmpp.XmppServiceBroadcastEventReceiver;

public class MyApplication extends Application {

    private final static String TAG = "MyApplication";

    private static MyApplication mInstance ;
    private static ChatServiceConnection mChatServiceConnection;
    private Intent mIntent;
    private static String username;
    private static String password;
    private static String hostname;
    private static String servicename;
    private static XmppRosterEntry rosterEntry;
    private static boolean isBOSH;
    private static boolean isSync;
    private boolean bindded;
    private XmppAccount mXmppAccount;
    private XmppServiceBroadcastEventReceiver receiver;
    private MainActivity mActivity;

    public static synchronized MyApplication getInstance () {
        return mInstance ;
    }

    @Override
    public void onCreate () {
        super.onCreate ();
        mInstance = this;
        bindded = false;
        XmppServiceBroadcastEventEmitter.initialize(this, "tk.munditv.mundichat");
        receiver = new XmppServiceBroadcastEventReceiver();
        receiver.register(this);
        RefreshPreference();
        initService();
    }

    public void setRosterEntry(XmppRosterEntry entry) {
        rosterEntry = entry;
    }

    public XmppRosterEntry getRosterEntry() {
        return rosterEntry;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getHostname() {
        return hostname;
    }

    public String getServicename() {
        return servicename;
    }

    public boolean isBOSH() {
        return isBOSH;
    }

    public boolean isHttps() {
        return true;
    }

    public boolean isSync() {
        return isSync;
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
        mXmppAccount.setResourceName("MundiChat");
        mXmppAccount.setPersonalMessage("Working!");
    }

    private void initService() {
        mIntent = new Intent(this, ChatService.class);
        mIntent.setAction(ActionStrings.ACTION_START_SERVICE);
        mChatServiceConnection = new ChatServiceConnection();
        boolean ret = bindService(mIntent, mChatServiceConnection, BIND_AUTO_CREATE);
        if (!ret) {
            Toast.makeText(this, "Service can not binded", 3);
            return;
        }
        startService(mIntent);
    }

    public void closeService() {
        unbindService(mChatServiceConnection);
        mChatServiceConnection = null;
        stopService(mIntent);
        mIntent = null;
        receiver.unregister(this);
    }

    public XmppServiceBroadcastEventReceiver getBroadcastReceiver() {
        return receiver;
    }

    public void doConnect() throws RemoteException {
        mChatServiceConnection.doConnect(mXmppAccount.toString());
    }

    public void doDisconnect() throws RemoteException {
        mChatServiceConnection.doDisconnect();
    }

    public void doLogin() throws RemoteException {
        mChatServiceConnection.login(mXmppAccount.toString());
    }

    public void doRegister() throws RemoteException {
        mChatServiceConnection.register(mXmppAccount.toString());
    }

    public boolean isConnected() throws RemoteException {
        return mChatServiceConnection.isConnect();
    }

    public boolean isAuthenticated() throws RemoteException {
        return mChatServiceConnection.isAuthenticated();
    }

    public void setPresence(int mode, String str) throws RemoteException {
        mChatServiceConnection.setPresence(mode, str);
    }

    public String doGetRosterEntries() throws RemoteException {
        return mChatServiceConnection.getRosterEntries();
    }

    public void doRefreshContact(String remoteAccount) throws RemoteException {
        mChatServiceConnection.refreshContact(remoteAccount);
    }

    public void doAddContact(String remoteAccount, String alias) throws RemoteException {
        mChatServiceConnection.addContact(remoteAccount, alias);
    }

    public void doRemoveContact(String remoteAccount) throws RemoteException {
        mChatServiceConnection.removeContact(remoteAccount);
    }

    public void doRenameContact(String remoteAccount, String alias) throws RemoteException {
        mChatServiceConnection.renameContact(remoteAccount, alias);
    }

    public void doSendMessage(String remoteAccount, String message) throws RemoteException {
        mChatServiceConnection.sendMessage(remoteAccount, message);
    }

    public void doDeleteMessage(long message_id) throws RemoteException {
        mChatServiceConnection.deleteMessage(message_id);
    }

    public void doSendPendingMessage() throws RemoteException {
        mChatServiceConnection.sendPendingMessage();
    }

    public void doSetAvatar(String path) throws RemoteException {
        mChatServiceConnection.setAvatar(path);
    }

    public void doClearConversations(String remoteAccount) throws RemoteException {
        mChatServiceConnection.clearConversations(remoteAccount);
    }

    public String doGetDatabaseName() throws RemoteException {
        return mChatServiceConnection.getDatabaseName();
    }

    public void setMainActivity(MainActivity mActivity) {
        this.mActivity = mActivity;
    }

    public void setTitle(String title) {
        mActivity.setTitleName(title);
    }

    public boolean hasBind() {
        return bindded;
    }

    private class ChatServiceConnection implements ServiceConnection {

        private IChatServiceInterface mInterface = null;

        private final static String TAG1 = "ChatServiceConnection";

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Logger.info(TAG1, "onServiceConnected() componentName = "
                    + componentName.toString());
            mInterface = IChatServiceInterface.Stub.asInterface(iBinder);
            bindded = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Logger.info(TAG1, "onServiceDisconnected() componentName = "
                    + componentName.toString());
            mInterface = null;
        }

        @Override
        public void onBindingDied(ComponentName name) {
            Logger.info(TAG1, "onBindingDied() componentName = "
                    + name.toString());
            mInterface = null;
        }

        @Override
        public void onNullBinding(ComponentName name) {
            Logger.info(TAG1, "onNullBinding() componentName = "
                    + name.toString());
        }

        public void doConnect(String jsonAccount) throws RemoteException {
            if (mInterface != null) mInterface.connect(jsonAccount);
        }

        public void doDisconnect() throws RemoteException {
            if (mInterface != null) mInterface.disconnect();
        }

        public boolean isConnect() throws RemoteException {
            if (mInterface != null) return mInterface.isConnected();
            return false;
        }

        public boolean isAuthenticated() throws RemoteException {
            if (mInterface != null) return mInterface.isAuthenticated();
            return false;
        }
        public void sendMessage(String remoteAccount, String message) throws RemoteException {
            if (mInterface != null) mInterface.sendMessage(remoteAccount, message);
        }

        public void deleteMessage(long message_id) throws RemoteException {
            if (mInterface != null) mInterface.deleteMessage(message_id);
        }

        public void sendPendingMessage() throws RemoteException {
            if (mInterface != null) mInterface.sendPendingMessage();
        }

        public void login(String jsonAccount) throws RemoteException {
            if (mInterface != null) mInterface.login(jsonAccount);
        }

        public void register(String jsonAccount) throws RemoteException {
            if (mInterface != null) mInterface.register(jsonAccount);
        }

        public void addContact(String remoteAccount, String alias) throws RemoteException {
            if (mInterface != null) mInterface.addContact(remoteAccount, alias);
        }

        public void removeContact(String remoteAccount) throws RemoteException {
            if (mInterface != null) mInterface.removeContact(remoteAccount);
        }

        public void renameContact(String remoteAccount, String alias) throws RemoteException {
            if (mInterface != null) mInterface.renameContact(remoteAccount, alias);
        }

        public void refreshContact(String remoteAccount) throws RemoteException {
            if (mInterface != null) mInterface.refreshContact(remoteAccount);
        }

        public void clearConversations(String remoteAccount) throws RemoteException {
            if (mInterface != null) mInterface.clearConversations(remoteAccount);
        }

        public void setAvatar(String path) throws RemoteException {
            if (mInterface != null) mInterface.setAvatar(path);
        }

        public void setPresence(int mode, String str) throws RemoteException {
            if (mInterface != null) mInterface.setPresence(mode, str);
        }

        public String getRosterEntries() throws RemoteException {
            if (mInterface != null)
                return mInterface.getServiceRosterEntries();
            return null;
        }

        public String getDatabaseName() throws RemoteException {
            if (mInterface != null)
                return mInterface.getDatabaseName();
            return null;
        }

    }

}
