package tk.munditv.xmppservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import static tk.munditv.xmppservice.XmppServiceBroadcastEventEmitter.BROADCAST_CONNECTED;
import static tk.munditv.xmppservice.XmppServiceBroadcastEventEmitter.BROADCAST_CONTACT_ADDED;
import static tk.munditv.xmppservice.XmppServiceBroadcastEventEmitter.BROADCAST_CONTACT_ADD_ERROR;
import static tk.munditv.xmppservice.XmppServiceBroadcastEventEmitter.BROADCAST_CONTACT_REMOVED;
import static tk.munditv.xmppservice.XmppServiceBroadcastEventEmitter.BROADCAST_CONTACT_RENAMED;
import static tk.munditv.xmppservice.XmppServiceBroadcastEventEmitter.BROADCAST_CONVERSATIONS_CLEARED;
import static tk.munditv.xmppservice.XmppServiceBroadcastEventEmitter.BROADCAST_CONVERSATIONS_CLEAR_ERROR;
import static tk.munditv.xmppservice.XmppServiceBroadcastEventEmitter.BROADCAST_DISCONNECTED;
import static tk.munditv.xmppservice.XmppServiceBroadcastEventEmitter.BROADCAST_MESSAGE_ADDED;
import static tk.munditv.xmppservice.XmppServiceBroadcastEventEmitter.BROADCAST_MESSAGE_DELETED;
import static tk.munditv.xmppservice.XmppServiceBroadcastEventEmitter.BROADCAST_MESSAGE_SENT;
import static tk.munditv.xmppservice.XmppServiceBroadcastEventEmitter.BROADCAST_ROSTER_CHANGED;
import static tk.munditv.xmppservice.XmppServiceBroadcastEventEmitter.PARAM_ALIAS;
import static tk.munditv.xmppservice.XmppServiceBroadcastEventEmitter.PARAM_INCOMING;
import static tk.munditv.xmppservice.XmppServiceBroadcastEventEmitter.PARAM_MESSAGE_ID;
import static tk.munditv.xmppservice.XmppServiceBroadcastEventEmitter.PARAM_REMOTE_ACCOUNT;

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

    private XmppServiceCallback mCallback;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;

        String action = intent.getAction();

        if (action == null) return;

        action = action.replace(XmppServiceBroadcastEventEmitter.getNamespace(), "");

        if (BROADCAST_CONNECTED.equals(action)) {
            onXmppConnected();

        } else if (BROADCAST_DISCONNECTED.equals(action)) {
            onXmppDisconnected();

        } else if (BROADCAST_ROSTER_CHANGED.equals(action)) {
            onRosterChanged();

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

        context.registerReceiver(this, intentFilter);
    }

    public void setCallback(XmppServiceCallback callback) {
        mCallback = callback;
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
        try {
            context.unregisterReceiver(this);
        } catch (Exception ignored) {}
    }

    /**
     * Called when the XMPP connection to the server has been established.
     */
    public void onXmppConnected() {
        if (mCallback != null) {
            mCallback.onConnected();
        }
    }

    /**
     * Called when the service is not connected to the XMPP server.
     */
    public void onXmppDisconnected() {
        if (mCallback != null) {
            mCallback.onDisconnected();
        }
    }

    /**
     * Called when the roster has been changed (contacts added, removed or change of
     * presence status). To get the current roster, use: {@link XmppService#getRosterEntries()}
     */
    public void onRosterChanged() {
        if (mCallback != null) {
            mCallback.onRosterChanged();
        }
    }

    /**
     * Called when a message has been added to the conversation with a contact
     * (either scheduled for sending or received)
     * @param remoteAccount xmpp JID of the contact
     * @param incoming true if the message is incoming, false if it's scheduled for sending
     */
    public void onMessageAdded(String remoteAccount, boolean incoming) {
        if (mCallback != null) {
            mCallback.onMessageAdded(remoteAccount, incoming);
        }
    }

    /**
     * Called when a contact has been successfully added to the roster, as a result of
     * {@link XmppServiceCommand#addContactToRoster(Context, String, String)}
     * @param remoteAccount xmpp JID of the contact
     */
    public void onContactAdded(String remoteAccount) {
        if (mCallback != null) {
            mCallback.onContactAdded(remoteAccount);
        }
    }

    /**
     * Called when a contact has been successfully removed from the roster, as a result of
     * {@link XmppServiceCommand#removeContactFromRoster(Context, String)}
     * @param remoteAccount xmpp JID of the contact
     */
    public void onContactRemoved(String remoteAccount) {
        if (mCallback != null) {
            mCallback.onContactRemoved(remoteAccount);
        }
    }

    /**
     * Called when a contact has been successfully renamed in the roster, as a result of
     * {@link XmppServiceCommand#renameContact(Context, String, String)}
     * @param remoteAccount xmpp JID of the contact
     */
    public void onContactRenamed(String remoteAccount, String newAlias) {
        if (mCallback != null) {
            mCallback.onContactRenamed(remoteAccount, newAlias);
        }
    }

    /**
     * Called when an error happened while trying to add a contact to the roster.
     * @param remoteAccount xmpp JID of the contact
     */
    public void onContactAddError(String remoteAccount) {
        if (mCallback != null) {
            mCallback.onContactAddError(remoteAccount);
        }
    }

    /**
     * Called when all the messages exchanged (sent and received) with a contact have been deleted,
     * as a result of {@link XmppServiceCommand#clearConversations(Context, String)}
     * @param remoteAccount xmpp JID of the contact
     */
    public void onConversationsCleared(String remoteAccount) {
        if (mCallback != null) {
            mCallback.onConversationsCleared(remoteAccount);
        }
    }

    /**
     * Called when an error happened during the deletion of the messages exchanged with a contact.
     * @param remoteAccount xmpp JID of the contact
     */
    public void onConversationsClearError(String remoteAccount) {
        if (mCallback != null) {
            mCallback.onConversationsClearError(remoteAccount);
        }
    }

    /**
     * Called when a message has been successfully sent.
     * @param messageId unique ID of the sent message
     */
    public void onMessageSent(long messageId) {
        if (mCallback != null) {
            mCallback.onMessageSent(messageId);
        }
    }

    /**
     * Called when a message has been successfully deleted from the xmpp service database.
     * @param messageId unique ID of the deleted message
     */
    public void onMessageDeleted(long messageId) {
        if (mCallback != null) {
            mCallback.onMessageDeleted(messageId);
        }
    }

}
