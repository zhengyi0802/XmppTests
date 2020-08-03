package tk.munditv.mundichat.xmpp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.RemoteException;

import tk.munditv.mundichat.callback.RosterListener;
import tk.munditv.mundichat.callback.PresenceListener;
import tk.munditv.mundichat.callback.MessageListener;
import tk.munditv.mundichat.utils.Logger;

import static tk.munditv.mundichat.xmpp.XmppServiceBroadcastEventEmitter.BROADCAST_CONNECTED;
import static tk.munditv.mundichat.xmpp.XmppServiceBroadcastEventEmitter.BROADCAST_CONTACT_ADDED;
import static tk.munditv.mundichat.xmpp.XmppServiceBroadcastEventEmitter.BROADCAST_CONTACT_ADD_ERROR;
import static tk.munditv.mundichat.xmpp.XmppServiceBroadcastEventEmitter.BROADCAST_CONTACT_REMOVED;
import static tk.munditv.mundichat.xmpp.XmppServiceBroadcastEventEmitter.BROADCAST_CONTACT_RENAMED;
import static tk.munditv.mundichat.xmpp.XmppServiceBroadcastEventEmitter.BROADCAST_CONVERSATIONS_CLEARED;
import static tk.munditv.mundichat.xmpp.XmppServiceBroadcastEventEmitter.BROADCAST_CONVERSATIONS_CLEAR_ERROR;
import static tk.munditv.mundichat.xmpp.XmppServiceBroadcastEventEmitter.BROADCAST_DISCONNECTED;
import static tk.munditv.mundichat.xmpp.XmppServiceBroadcastEventEmitter.BROADCAST_GET_ROSTER_ENTRIES;
import static tk.munditv.mundichat.xmpp.XmppServiceBroadcastEventEmitter.BROADCAST_MESSAGE_ADDED;
import static tk.munditv.mundichat.xmpp.XmppServiceBroadcastEventEmitter.BROADCAST_MESSAGE_DELETED;
import static tk.munditv.mundichat.xmpp.XmppServiceBroadcastEventEmitter.BROADCAST_MESSAGE_SENT;
import static tk.munditv.mundichat.xmpp.XmppServiceBroadcastEventEmitter.BROADCAST_ROSTER_CHANGED;
import static tk.munditv.mundichat.xmpp.XmppServiceBroadcastEventEmitter.PARAM_ALIAS;
import static tk.munditv.mundichat.xmpp.XmppServiceBroadcastEventEmitter.PARAM_INCOMING;
import static tk.munditv.mundichat.xmpp.XmppServiceBroadcastEventEmitter.PARAM_MESSAGE_ID;
import static tk.munditv.mundichat.xmpp.XmppServiceBroadcastEventEmitter.PARAM_REMOTE_ACCOUNT;

/**
 * Broadcast receiver to sublass to create a receiver for
 * {@link XmppService} events.
 *
 * It provides the boilerplate code to properly handle broadcast messages coming from the
 * XMPP service and dispatch them to the proper handler method.
 *
 * @author gotev (Aleksandar Gotev)
 */
public class XmppServiceBroadcastEventReceiver extends BroadcastReceiver {

    private static final String TAG = "XmppServiceBroadcastEventReceiver";

    private PresenceListener mPresenceCallback;
    private RosterListener mRosterCallback;
    private MessageListener mMessageCallback;
    private Context mContext;

    @Override
    public void onReceive(Context context, Intent intent) {
        //Logger.debug(TAG,"onReceive()");
        if (intent == null) return;

        String action = intent.getAction();

        if (action == null) return;

        action = action.replace(XmppServiceBroadcastEventEmitter.getNamespace(), "");

        if (BROADCAST_CONNECTED.equals(action)) {
            try {
                onXmppConnected();
            } catch (RemoteException e) {
                e.printStackTrace();
            }

        } else if (BROADCAST_DISCONNECTED.equals(action)) {
            onXmppDisconnected();

        } else if (BROADCAST_ROSTER_CHANGED.equals(action)) {
            try {
                onRosterChanged();
            } catch (RemoteException e) {
                e.printStackTrace();
            }

        } else if (BROADCAST_MESSAGE_ADDED.equals(action)) {
            onMessageAdded(intent.getStringExtra(PARAM_REMOTE_ACCOUNT),
                           intent.getBooleanExtra(PARAM_INCOMING, false));

        } else if (BROADCAST_CONTACT_ADDED.equals(action)) {
            onContactAdded(intent.getStringExtra(PARAM_REMOTE_ACCOUNT));

        } else if (BROADCAST_CONTACT_ADD_ERROR.equals(action)) {
            onContactAddError(intent.getStringExtra(PARAM_REMOTE_ACCOUNT));

        } else if (BROADCAST_CONVERSATIONS_CLEARED.equals(action)) {
            onConversationsCleared(intent.getStringExtra(PARAM_REMOTE_ACCOUNT));

        } else if (BROADCAST_CONVERSATIONS_CLEAR_ERROR.equals(action)) {
            onConversationsClearError(intent.getStringExtra(PARAM_REMOTE_ACCOUNT));

        } else if (BROADCAST_CONTACT_RENAMED.equals(action)) {
            onContactRenamed(intent.getStringExtra(PARAM_REMOTE_ACCOUNT),
                             intent.getStringExtra(PARAM_ALIAS));

        } else if (BROADCAST_CONTACT_REMOVED.equals(action)) {
            onContactRemoved(intent.getStringExtra(PARAM_REMOTE_ACCOUNT));

        } else if (BROADCAST_MESSAGE_SENT.equals(action)) {
            long msgId = intent.getLongExtra(PARAM_MESSAGE_ID, -1);
            if (msgId > 0) {
                onMessageSent(msgId);
            }

        } else if (BROADCAST_MESSAGE_DELETED.equals(action)) {
            long msgId = intent.getLongExtra(PARAM_MESSAGE_ID, -1);

            if (msgId > 0) {
                onMessageDeleted(msgId);
            }
        }

    }

    /**
     * Register this receiver.<br>
     * If you use this receiver in an {@link android.app.Activity}, you have to call this method inside
     * {@link android.app.Activity#onResume()}, after {@code super.onResume();}.<br>
     * If you use it in a {@link android.app.Service}, you have to
     * call this method inside {@link android.app.Service#onCreate()}, after {@code super.onCreate();}.
     *
     * @param context context in which to register this receiver
     */
    public void register(Context context) {
        Logger.debug(TAG,"register()");

        final IntentFilter intentFilter = new IntentFilter();
        String namespace = XmppServiceBroadcastEventEmitter.getNamespace();

        intentFilter.addAction(namespace + BROADCAST_CONNECTED);
        intentFilter.addAction(namespace + BROADCAST_DISCONNECTED);
        intentFilter.addAction(namespace + BROADCAST_ROSTER_CHANGED);
        intentFilter.addAction(namespace + BROADCAST_MESSAGE_ADDED);
        intentFilter.addAction(namespace + BROADCAST_CONTACT_ADDED);
        intentFilter.addAction(namespace + BROADCAST_CONTACT_REMOVED);
        intentFilter.addAction(namespace + BROADCAST_CONTACT_ADD_ERROR);
        intentFilter.addAction(namespace + BROADCAST_CONVERSATIONS_CLEARED);
        intentFilter.addAction(namespace + BROADCAST_CONVERSATIONS_CLEAR_ERROR);
        intentFilter.addAction(namespace + BROADCAST_CONTACT_RENAMED);
        intentFilter.addAction(namespace + BROADCAST_MESSAGE_SENT);
        intentFilter.addAction(namespace + BROADCAST_MESSAGE_DELETED);
        intentFilter.addAction(namespace + BROADCAST_GET_ROSTER_ENTRIES);

        context.registerReceiver(this, intentFilter);

    }

    /**
     * Unregister this receiver.<br>
     * If you use this receiver in an {@link android.app.Activity}, you have to call this method inside
     * {@link android.app.Activity#onPause()}, after {@code super.onPause();}.<br>
     * If you use it in a {@link android.app.Service}, you have to
     * call this method inside {@link android.app.Service#onDestroy()}.
     *
     * @param context context in which to unregister this receiver
     */
    public void unregister(Context context) {
        Logger.debug(TAG,"unregister()");

        try {
            context.unregisterReceiver(this);
        } catch (Exception ignored) {}
    }

    public void registerPresenceCallback(Context context, PresenceListener callback) {
        mContext = context;
        mPresenceCallback = callback;
    }

    public void registerRosterCallback(Context context, RosterListener callback) {
        mContext = context;
        mRosterCallback = callback;
    }

    public void registerMessageCallback(Context context, MessageListener callback) {
        mContext = context;
        mMessageCallback = callback;
    }

    /**
     * Called when the XMPP connection to the server has been established.
     */
    public void onXmppConnected() throws RemoteException {
        Logger.debug(TAG,"onXmppConnected()");
        if (mPresenceCallback != null) mPresenceCallback.onConnected();
    }

    /**
     * Called when the service is not connected to the XMPP server.
     */
    public void onXmppDisconnected() {
        Logger.debug(TAG,"onXmppDisconnected()");
        if (mPresenceCallback != null) mPresenceCallback.onDisconnected();
    }

    /**
     * Called when the roster has been changed (contacts added, removed or change of
     * presence status). To get the current roster, use: {@link XmppService#getRosterEntries()}
     */
    public void onRosterChanged() throws RemoteException {
        Logger.debug(TAG,"onRosterChanged()");
        if (mRosterCallback != null) mRosterCallback.onRosterChanged();
    }

    /**
     * Called when a message has been added to the conversation with a contact
     * (either scheduled for sending or received)
     * @param remoteAccount xmpp JID of the contact
     * @param incoming true if the message is incoming, false if it's scheduled for sending
     */
    public void onMessageAdded(String remoteAccount, boolean incoming) {
        Logger.debug(TAG, "onMessageAdded()");
        Logger.debug(TAG, "remote account= " + remoteAccount);
        if (mMessageCallback != null) mMessageCallback.onMessageAdded(remoteAccount, incoming);
    }

    /**
     * Called when a contact has been successfully added to the roster, as a result of
     * {@link XmppServiceCommand#addContactToRoster(Context, String, String)}
     * @param remoteAccount xmpp JID of the contact
     */
    public void onContactAdded(String remoteAccount) {
        Logger.debug(TAG,"onContactAdded()");
        Logger.debug(TAG, "remote account= " + remoteAccount);
        if (mRosterCallback != null) mRosterCallback.onContactAdded(remoteAccount);
    }

    /**
     * Called when a contact has been successfully removed from the roster, as a result of
     * {@link XmppServiceCommand#removeContactFromRoster(Context, String)}
     * @param remoteAccount xmpp JID of the contact
     */
    public void onContactRemoved(String remoteAccount) {
        Logger.debug(TAG,"onContactRemoved()");
        Logger.debug(TAG, "remote account= " + remoteAccount);
        if (mRosterCallback != null) mRosterCallback.onContactRemoved(remoteAccount);
    }

    /**
     * Called when a contact has been successfully renamed in the roster, as a result of
     * {@link XmppServiceCommand#renameContact(Context, String, String)}
     * @param remoteAccount xmpp JID of the contact
     */
    public void onContactRenamed(String remoteAccount, String newAlias) {
        Logger.debug(TAG,"onContactRenamed()");
        Logger.debug(TAG, "remote account= " + remoteAccount);
        Logger.debug(TAG, "new Alias= " + newAlias);
        if (mRosterCallback != null) mRosterCallback.onContactRenamed(remoteAccount, newAlias);
    }

    /**
     * Called when an error happened while trying to add a contact to the roster.
     * @param remoteAccount xmpp JID of the contact
     */
    public void onContactAddError(String remoteAccount) {
        Logger.debug(TAG,"onContactAddError()");
        if (mRosterCallback != null) mRosterCallback.onContactAddError(remoteAccount);
    }

    /**
     * Called when all the messages exchanged (sent and received) with a contact have been deleted,
     * as a result of {@link XmppServiceCommand#clearConversations(Context, String)}
     * @param remoteAccount xmpp JID of the contact
     */
    public void onConversationsCleared(String remoteAccount) {
        Logger.debug(TAG,"onConversationsCleared()");
        if (mMessageCallback != null) mMessageCallback.onConversationsCleared(remoteAccount);
    }

    /**
     * Called when an error happened during the deletion of the messages exchanged with a contact.
     * @param remoteAccount xmpp JID of the contact
     */
    public void onConversationsClearError(String remoteAccount) {
        Logger.debug(TAG,"onConversationsClearError()");
        if (mMessageCallback != null) mMessageCallback.onConversationsClearError(remoteAccount);
    }

    /**
     * Called when a message has been successfully sent.
     * @param messageId unique ID of the sent message
     */
    public void onMessageSent(long messageId) {
        Logger.debug(TAG,"onMessageSent()");
        if (mMessageCallback != null) mMessageCallback.onMessageSent(messageId);
    }

    /**
     * Called when a message has been successfully deleted from the xmpp service database.
     * @param messageId unique ID of the deleted message
     */
    public void onMessageDeleted(long messageId) {
        Logger.debug(TAG,"onMessageDeleted()");
        if (mMessageCallback != null) mMessageCallback.onMessageDeleted(messageId);
    }
/*
    public void onGetRosterEntries() throws RemoteException {
        Logger.debug(TAG,"onGetRosterEntries()");
        if (mRosterCallback != null) mRosterCallback.onGetRosterEntries();
    }
*/
}
