package tk.munditv.mundichat.service;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.RemoteException;

import com.google.gson.Gson;

import org.jivesoftware.smack.SmackConfiguration;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.ArrayList;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import tk.munditv.mundichat.IChatServiceInterface;
import tk.munditv.mundichat.database.SqLiteDatabase;
import tk.munditv.mundichat.database.providers.MessagesProvider;
import tk.munditv.mundichat.utils.Logger;
import tk.munditv.mundichat.xmpp.XmppAccount;
import tk.munditv.mundichat.xmpp.XmppRosterEntry;
import tk.munditv.mundichat.xmpp.XmppServiceBroadcastEventEmitter;
import tk.munditv.mundichat.xmpp.XmppServiceConnection;

public class ChatService extends BackgroundService {

    private final static String TAG = "ChatService";
    public static final String DATABASE_NAME = "messages.db";
    public static final String PREFS_NAME = "messages-prefs.conf";
    private static final String KEY_CONFIGURED_ACCOUNT = "configuredAccount";
    public static int CONNECT_TIMEOUT = 5000;
    public static int PACKET_REPLY_TIMEOUT = 5000;
    public static int DEFAULT_PING_INTERVAL = 600; //10 minutes;
    public static boolean USE_STREAM_MANAGEMENT = true;
    public static int STREAM_MANAGEMENT_RESUMPTION_TIME = 30;
    public static SSLContext CUSTOM_SSL_CONTEXT = null;
    public static HostnameVerifier CUSTOM_HOSTNAME_VERIFIER = null;
    private SharedPreferences mPrefs;
    private XmppServiceConnection mConnection;
    private static volatile SqLiteDatabase mDatabase;
    private static volatile boolean connected = false;
    private static volatile boolean authenticated = false;
    private static volatile ArrayList<XmppRosterEntry> rosterEntries;

    public static ArrayList<XmppRosterEntry> getRosterEntries() {
        synchronized (ChatService.class) {
            Logger.debug(TAG, "getRosterEntries()");
            return rosterEntries;
        }
    }

    public static void setRosterEntries(ArrayList<XmppRosterEntry> entries) {
        synchronized (ChatService.class) {
            Logger.debug(TAG, "setRosterEntries()");
            rosterEntries = entries;
        }
    }

    public static synchronized boolean isConnected() {
        return connected;
    }

    public static synchronized void setConnected(boolean newConnectedStatus) {
        Logger.debug(TAG, "setConnected("+ newConnectedStatus + ")");
        connected = newConnectedStatus;
        if(!connected) {
            authenticated = false;
        }
                if (newConnectedStatus) {
            XmppServiceBroadcastEventEmitter.broadcastXmppConnected();
        } else {
            XmppServiceBroadcastEventEmitter.broadcastXmppDisconnected();
        }
   }

    public static synchronized void setAuthenticated(boolean newAuthenticatedStatus) {
        Logger.debug(TAG, "setAuthenticated(" + newAuthenticatedStatus + ")");
        authenticated = newAuthenticatedStatus;
        return;
    }

    public static synchronized SqLiteDatabase getDatabase() {
        Logger.debug(TAG, "getDatabase()");
        return mDatabase;
    }

    public static synchronized String dogetDatabaseName() {
        return mDatabase.getDatabaseName();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        enqueueJob(new Runnable() {
            @Override
            public void run() {
                mPrefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                //SmackConfiguration.setDefaultPacketReplyTimeout(PACKET_REPLY_TIMEOUT);

                Logger.info(TAG, "initializing database");
                mDatabase = new SqLiteDatabase(ChatService.this, DATABASE_NAME);
            }
        });
    }

    @Override
    public IBinder onBind(Intent intent) {
        //return super.onBind(intent);
        Logger.debug(TAG, "onBind()");
        return mStub;
    }

    @Override
    public void onDestroy() {
        if (mDatabase != null) {
            mDatabase.close();
            mDatabase = null;
        }

        if (mConnection != null) {
            mConnection.disconnect();
            mConnection = null;
        }

        connected = false;
        rosterEntries = null;

        super.onDestroy();
    }

    @Override
    protected void enqueueJob(Runnable job) {
        super.enqueueJob(job);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    private IChatServiceInterface.Stub mStub = new IChatServiceInterface.Stub() {
        @Override
        public int getPid() throws RemoteException {
            return 0;
        }

        @Override
        public void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat,
                               double aDouble, String aString) throws RemoteException {
        }

        @Override
        public void connect(final String jsonAccount) throws RemoteException {
            //Logger.debug(TAG, "mStub::connect()");
            enqueueJob(new Runnable(){

                @Override
                public void run() {
                    Logger.debug(TAG, "mStub::connect()");
                    XmppAccount xmppAccount = XmppAccount.fromJson(jsonAccount);
                    handleConnect(xmppAccount);
                }
            });
        }

        @Override
        public void disconnect() throws RemoteException {
            Logger.debug(TAG, "mStub::disconnect()");
            enqueueJob(new Runnable(){

                @Override
                public void run() {
                    handleDisconnect();
                }
            });
        }

        @Override
        public boolean isConnected() throws RemoteException {
            Logger.debug(TAG, "mStub::isConnected()");
            return connected;
        }

        @Override
        public boolean isAuthenticated() throws RemoteException {
            Logger.debug(TAG, "mStub::isAuthenticated()");
            return authenticated;
        }

        @Override
        public String getDatabaseName() throws RemoteException {
            return dogetDatabaseName();
        }

        @Override
        public void login(final String jsonAccount) throws RemoteException {

            enqueueJob(new Runnable(){

                @Override
                public void run() {
                    XmppAccount xmppAccount = XmppAccount.fromJson(jsonAccount);
                    try {
                        Logger.debug(TAG, "mStub::login(\""
                                + xmppAccount.getXmppJid().toString()
                                + "\", \"" + xmppAccount.getPassword() + "\")");
                        handleLogin(xmppAccount);
                    } catch (XmppStringprepException e) {
                        e.printStackTrace();
                    }

                }
            });
        }

        @Override
        public void register(final String jsonAccount) throws RemoteException {

            enqueueJob(new Runnable(){

                @Override
                public void run() {
                    XmppAccount xmppAccount = XmppAccount.fromJson(jsonAccount);
                    try {
                        Logger.debug(TAG, "mStub::register(\""
                                + xmppAccount.getXmppJid().toString()
                                + "\", \"" + xmppAccount.getPassword() + "\")");
                        handleRegister(xmppAccount);
                    } catch (XmppStringprepException e) {
                        e.printStackTrace();
                    }

                }
            });
        }

        @Override
        public void setPresence(final int mode, final String str) throws RemoteException {
            enqueueJob(new Runnable() {

                @Override
                public void run() {
                    handleSetPresence(mode, str);
                }
            });
        }

        @Override
        public String getServiceRosterEntries() {
            Logger.debug(TAG,"getServiceRosterEntries()");
            if (rosterEntries == null) return null;
            try {
                String str = new Gson().toJson(rosterEntries);
                for (XmppRosterEntry entry : rosterEntries) {
                    Logger.debug(TAG, " entry XmppJID = " + entry.getXmppJID());
                    Logger.debug(TAG, " entry Alias = " + entry.getAlias());
                    Logger.debug(TAG, " entry PresenceMode = " + entry.getPresenceMode());
                    Logger.debug(TAG, " entry PersonalMessage = " + entry.getPersonalMessage());
                    Logger.debug(TAG, " entry UnRead Messages = " + entry.getUnreadMessages());
                    Logger.debug(TAG, " entry Available = " + entry.isAvailable());
                    return str;
                }
                Logger.debug(TAG, "str = " + str);
                Logger.debug(TAG, "size = " + rosterEntries.size());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        public void refreshContact(final String remoteAccount) throws RemoteException {
            enqueueJob(new Runnable() {
                @Override
                public void run() {
                    try {
                        handleRefreshContact(remoteAccount);
                    } catch (XmppStringprepException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void sendMessage(final String remoteAccount, final String message) throws RemoteException {
            enqueueJob(new Runnable() {
                @Override
                public void run() {
                    handleSendMessage(remoteAccount, message);
                }
            });
        }

        @Override
        public void deleteMessage(final long message_id) throws RemoteException {
            enqueueJob(new Runnable() {
                @Override
                public void run() {
                    handleDeleteMessage(message_id);
                }
            });
        }

        @Override
        public void sendPendingMessage() throws RemoteException {
            enqueueJob(new Runnable() {
                @Override
                public void run() {
                    handleSendPendingMessages();
                }
            });
        }

        @Override
        public void addContact(final String remoteAccount, final String alias) throws RemoteException {
            enqueueJob(new Runnable() {
                @Override
                public void run() {
                    handleAddContact(remoteAccount, alias);
                }
            });
        }

        @Override
        public void removeContact(final String remoteAccount) throws RemoteException {
            enqueueJob(new Runnable() {
                @Override
                public void run() {
                    handleRemoveContact(remoteAccount);
                }
            });
        }

        @Override
        public void renameContact(final String remoteAccount, final String alias) throws RemoteException {
            enqueueJob(new Runnable() {
                @Override
                public void run() {
                    handleRenameContact(remoteAccount, alias);
                }
            });
        }

        @Override
        public void clearConversations(final String remoteAccount) throws RemoteException {
            enqueueJob(new Runnable() {
                @Override
                public void run() {
                    try {
                        handleClearConversations(remoteAccount);
                    } catch (XmppStringprepException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void setAvatar(final String path) throws RemoteException {
            enqueueJob(new Runnable() {
                @Override
                public void run() {
                    handleSetAvatar(path);
                }
            });
        }

    };

    private void handleConnect(XmppAccount account) {
        try {
            if (mConnection != null) {
                mConnection.disconnect();
            }

            mConnection = new XmppServiceConnection(account, mDatabase);
            mConnection.connect();
            mConnection.sendPendingMessages();
            mPrefs.edit().putString(KEY_CONFIGURED_ACCOUNT, account.toString()).apply();
        } catch (Exception exc) {
            Logger.error(TAG, "Error while connecting", exc);
            XmppServiceBroadcastEventEmitter.broadcastXmppDisconnected();
        }
    }

    public void handleLogin(XmppAccount account) {
        try {
            if (mConnection == null) {
                mConnection = new XmppServiceConnection(account, mDatabase);
                mConnection.connect();
                mConnection.sendPendingMessages();
                mPrefs.edit().putString(KEY_CONFIGURED_ACCOUNT, account.toString()).apply();
            }
            mConnection.doLogin();
        } catch (Exception exc) {
            Logger.error(TAG, "Error while connecting", exc);
            XmppServiceBroadcastEventEmitter.broadcastXmppDisconnected();
        }
    }

    public void handleRegister(XmppAccount account) {
        try {
            if (mConnection == null) {
                mConnection = new XmppServiceConnection(account, mDatabase);
                mConnection.connect();
            }
            mConnection.register();
        } catch (Exception exc) {
            Logger.error(TAG, "Error while connecting", exc);
            XmppServiceBroadcastEventEmitter.broadcastXmppDisconnected();
        }
    }

    private void handleDisconnect() {
        if (mConnection != null) {
            mConnection.disconnect();
        }

        mPrefs.edit().remove(KEY_CONFIGURED_ACCOUNT).apply();
        stopSelf();
    }

    private void handleSendMessage(String remoteAccount, String message) {
        try {
            Jid jid = JidCreate.entityBareFrom(remoteAccount);
            mConnection.addMessageAndProcessPending(jid, message);

        } catch (Exception exc) {
            Logger.error(TAG, "Error while sending message to " + remoteAccount, exc);
        }
    }

    private void handleDeleteMessage(long messageID) {
        try {
            new MessagesProvider(mDatabase).deleteMessage(messageID).execute();
            XmppServiceBroadcastEventEmitter.broadcastMessageDeleted(messageID);

        } catch (Exception exc) {
            Logger.error(TAG, "Error while deleting message with ID=" + messageID, exc);
        }
    }

    private void handleSendPendingMessages() {
        try {
            mConnection.sendPendingMessages();
        } catch (Exception exc) {
            Logger.error(TAG, "Error while sending pending messages", exc);
        }
    }

    private void handleAddContact(String remoteAccount, String alias) {
        try {
            BareJid jid = JidCreate.entityBareFrom(remoteAccount);
            mConnection.addContact(jid, alias);

        } catch (Exception exc) {
            Logger.error(TAG, "Error while adding contact: " + remoteAccount, exc);
        }
    }

    private void handleRemoveContact(String remoteAccount) {
        try {
            BareJid jid = JidCreate.entityBareFrom(remoteAccount);
            mConnection.removeContact(jid);

        } catch (Exception exc) {
            Logger.error(TAG, "Error while removing contact: " + remoteAccount, exc);
        }
    }

    private void handleRenameContact(String remoteAccount, String alias) {
        try {
            BareJid jid = JidCreate.entityBareFrom(remoteAccount);
            mConnection.renameContact(jid, alias);

        } catch (Exception exc) {
            Logger.error(TAG, "Error while renaming contact: " + remoteAccount, exc);
        }
    }

    private void handleRefreshContact(String remoteAccount)
            throws XmppStringprepException {
        Logger.debug(TAG, "handleRefreshContact()");
        //String remoteAccount = intent.getStringExtra(PARAM_REMOTE_ACCOUNT);

        Jid jid = JidCreate.entityBareFrom(remoteAccount);
        if (mConnection != null)
            mConnection.singleEntryUpdated(jid);
    }

    private void handleClearConversations(String remoteAccount) throws XmppStringprepException {
        BareJid jid = JidCreate.entityBareFrom(remoteAccount);
        if (mConnection != null)
            mConnection.clearConversationsWith(jid);
    }

    private void handleSetAvatar(String avatarPath) {
        try {
            mConnection.setOwnAvatar(avatarPath);
        } catch (Exception exc) {
            Logger.error(TAG, "Error while setting own avatar", exc);
        }
    }

    private void handleSetPresence(int mode, String str) {
        Logger.debug(TAG, "handleSetPresence(" + mode + ", \"" + str + "\")");
        try {
            Jid jid = JidCreate.from(str);
            mConnection.setPresence(mode, jid);
        } catch (Exception exc) {
            Logger.error(TAG, "Error while setting presence", exc);
        }
    }

}
