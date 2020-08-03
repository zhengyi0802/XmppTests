package tk.munditv.xmppservice;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.RosterListener;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.ping.PingFailedListener;
import org.jivesoftware.smackx.ping.PingManager;
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager;
import org.jivesoftware.smackx.vcardtemp.VCardManager;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import tk.munditv.xmppservice.database.SqLiteDatabase;
import tk.munditv.xmppservice.database.TransactionBuilder;
import tk.munditv.xmppservice.database.providers.MessagesProvider;
import tk.munditv.xmppservice.database.tables.MessagesTable;

/**
 * Implementation of the XMPP Connection.
 * @author gotev (Aleksandar Gotev)
 */
public class XmppServiceConnection
        implements ConnectionListener, PingFailedListener, ChatMessageListener,
        ChatManagerListener, RosterListener {

    private static final String TAG = "connection";

    private XmppAccount mAccount;
    private byte[] mOwnAvatar;
    private SqLiteDatabase mDatabase;
    private XMPPTCPConnection mConnection;
    private MessagesProvider mMessagesProvider;

    public XmppServiceConnection(XmppAccount account, SqLiteDatabase database) {
        mAccount = account;
        mDatabase = database;
        mMessagesProvider = new MessagesProvider(mDatabase);
    }

    public void connect() throws IOException, XMPPException, SmackException {
        if (mConnection == null) {
            createConnection();
        }

        if (!mConnection.isConnected()) {
            Logger.info(TAG, "Connecting to " + mAccount.getHost() + ":" + mAccount.getPort());
            mConnection.connect();

            Roster roster = Roster.getInstanceFor(mConnection);
            roster.removeRosterListener(this);
            roster.addRosterListener(this);
            roster.setSubscriptionMode(Roster.SubscriptionMode.accept_all);
            roster.setRosterLoadedAtLogin(true);
        }

        if (!mConnection.isAuthenticated()) {
            Logger.info(TAG, "Authenticating " + mAccount.getXmppJid());
            mConnection.login();

            PingManager.setDefaultPingInterval(XmppService.DEFAULT_PING_INTERVAL);
            PingManager pingManager = PingManager.getInstanceFor(mConnection);
            pingManager.registerPingFailedListener(this);

            ChatManager chatManager = ChatManager.getInstanceFor(mConnection);
            chatManager.removeChatListener(this);
            chatManager.addChatListener(this);

            DeliveryReceiptManager receipts = DeliveryReceiptManager.getInstanceFor(mConnection);
            receipts.setAutoReceiptMode(DeliveryReceiptManager.AutoReceiptMode.always);
            receipts.autoAddDeliveryReceiptRequests();
        }

        mOwnAvatar = getAvatarFor("");
    }

    public void disconnect() {
        mConnection.disconnect();
    }

    private void createConnection() {
        Logger.debug(TAG, "creating new connection to " + mAccount.getHost() + ":" + mAccount.getPort());

        XMPPTCPConnectionConfiguration.Builder builder = XMPPTCPConnectionConfiguration.builder()
                .setServiceName(mAccount.getServiceName())
                .setResource(mAccount.getResourceName())
                .setHost(mAccount.getHost())
                .setPort(mAccount.getPort())
                .setUsernameAndPassword(mAccount.getXmppJid(), mAccount.getPassword())
                .setConnectTimeout(XmppService.CONNECT_TIMEOUT);

        if (XmppService.CUSTOM_SSL_CONTEXT != null) {
            Logger.debug(TAG, "setting custom SSL context");
            builder.setCustomSSLContext(XmppService.CUSTOM_SSL_CONTEXT);
        }

        if (XmppService.CUSTOM_HOSTNAME_VERIFIER != null) {
            Logger.debug(TAG, "setting custom hostname verifier");
            builder.setHostnameVerifier(XmppService.CUSTOM_HOSTNAME_VERIFIER);
        }

        mConnection = new XMPPTCPConnection(builder.build());
        mConnection.setUseStreamManagement(XmppService.USE_STREAM_MANAGEMENT);
        mConnection.setUseStreamManagementResumption(XmppService.USE_STREAM_MANAGEMENT);
        mConnection.setPreferredResumptionTime(XmppService.STREAM_MANAGEMENT_RESUMPTION_TIME);

        mConnection.addConnectionListener(this);
    }

    public void singleEntryUpdated(String entry) {
        if (entry == null || entry.isEmpty())
            return;

        List<String> entries = new ArrayList<>(1);
        entries.add(entry);
        entriesUpdated(entries);
    }

    public void addMessageAndProcessPending(String destinationJID, String message) throws SmackException.NotConnectedException {
        saveMessage(destinationJID, message, false);
        sendPendingMessages();
    }

    private void saveMessage(String remoteAccount, String message, boolean incoming) {
        try {
            MessagesProvider messagesProvider = new MessagesProvider(mDatabase);
            TransactionBuilder insertTransaction;

            if (incoming) {
                insertTransaction = messagesProvider.addIncomingMessage(mAccount.getXmppJid(), remoteAccount, message);
            } else {
                insertTransaction = messagesProvider.addOutgoingMessage(mAccount.getXmppJid(), remoteAccount, message);
            }

            insertTransaction.execute();
            if (incoming) {
                singleEntryUpdated(remoteAccount);
            }
            XmppServiceBroadcastEventEmitter.broadcastMessageAdded(remoteAccount, incoming);

        } catch (Exception exc) {
            Logger.error(TAG, "Error while saving " + (incoming ? "incoming" : "outgoing") +
                              " message " + (incoming ? "from " : "to ") + remoteAccount, exc);
        }
    }

    private void sendMessage(String destinationJID, String message) throws SmackException.NotConnectedException {
        Logger.debug(TAG, "Sending message to " + destinationJID);
        Chat chat = ChatManager.getInstanceFor(mConnection).createChat(destinationJID, this);
        chat.sendMessage(message);
    }

    public void sendPendingMessages() throws SmackException.NotConnectedException {
        Logger.debug(TAG, "Sending pending messages for " + mAccount.getXmppJid());

        MessagesProvider messagesProvider = new MessagesProvider(mDatabase);

        List<tk.munditv.xmppservice.database.models.Message> messages =
                messagesProvider.getPendingMessages(mAccount.getXmppJid());
        if (messages == null || messages.isEmpty()) return;

        for (tk.munditv.xmppservice.database.models.Message message : messages) {
            sendMessage(message.getRemoteAccount(), message.getMessage());
            messagesProvider.updateMessageStatus(message.getId(), MessagesTable.Status.SENT)
                            .execute();

            XmppServiceBroadcastEventEmitter.broadcastMessageSent(message.getId());
        }
    }

    public void setPresence(int presenceMode, String personalMessage) throws SmackException.NotConnectedException {
        mAccount.setPresenceMode(presenceMode);
        mAccount.setPersonalMessage(personalMessage);
        setPresence();
    }

    public void setPresence() throws SmackException.NotConnectedException {
        Presence.Mode presMode;

        switch(mAccount.getPresenceMode()) {
            case XmppAccount.PRESENCE_MODE_CHAT:
                presMode = Presence.Mode.chat;
                break;

            case XmppAccount.PRESENCE_MODE_AVAILABLE:
            default:
                presMode = Presence.Mode.available;
                break;

            case XmppAccount.PRESENCE_MODE_AWAY:
                presMode = Presence.Mode.away;
                break;

            case XmppAccount.PRESENCE_MODE_XA:
                presMode = Presence.Mode.xa;
                break;

            case XmppAccount.PRESENCE_MODE_DND:
                presMode = Presence.Mode.dnd;
                break;
        }

        mConnection.sendStanza(new Presence(Presence.Type.available, mAccount.getPersonalMessage(),
                mAccount.getPriority(), presMode));
    }

    public void setOwnAvatar(String filePath)
            throws SmackException.NotConnectedException, XMPPException.XMPPErrorException,
                   SmackException.NoResponseException {

        VCardManager manager = VCardManager.getInstanceFor(mConnection);

        VCard vcard = null;

        try {
            vcard = manager.loadVCard();
        } catch (Exception exc){ }

        if (vcard == null) {
            vcard = new VCard();
        }

        Bitmap newAvatar = BitmapFactory.decodeFile(filePath);
        Bitmap scaled = Utils.getScaledBitmap(newAvatar, 128);
        mOwnAvatar = Utils.getBytes(scaled);
        vcard.setAvatar(mOwnAvatar);
        manager.saveVCard(vcard);
    }

    private byte[] getAvatarFor(String remoteAccount) {
        try {
            VCardManager manager = VCardManager.getInstanceFor(mConnection);

            byte[] data;
            VCard card;
            if (remoteAccount == null || remoteAccount.isEmpty()) {
                card = manager.loadVCard();
            } else {
                card = manager.loadVCard(remoteAccount);
            }

            if (card == null) return null;

            data = card.getAvatar();

            if (data != null && data.length > 0) {
                return data;
            }

            return null;

        } catch (Exception exc) {
            Logger.debug(TAG, "Can't get vCard for " + remoteAccount);
            return null;
        }
    }

    public void addContact(String remoteAccount, String alias) {
        try {
            Roster.getInstanceFor(mConnection).createEntry(remoteAccount, alias, null);
            XmppServiceBroadcastEventEmitter.broadcastContactAdded(remoteAccount);

        } catch (Exception exc) {
            Logger.error(TAG, "Error while adding contact: " + remoteAccount, exc);
            XmppServiceBroadcastEventEmitter.broadcastContactAddError(remoteAccount);
        }
    }

    public void removeContact(String remoteAccount) {
        try {
            Roster roster = Roster.getInstanceFor(mConnection);
            RosterEntry entry = roster.getEntry(remoteAccount);
            roster.removeEntry(entry);

            clearConversationsWith(remoteAccount);

            XmppServiceBroadcastEventEmitter.broadcastContactRemoved(remoteAccount);

        } catch (Exception exc) {
            Logger.error(TAG, "Error while removing contact: " + remoteAccount, exc);
        }
    }

    public void renameContact(String remoteAccount, String newAlias) {
        try {
            Roster roster = Roster.getInstanceFor(mConnection);
            RosterEntry entry = roster.getEntry(remoteAccount);
            entry.setName(newAlias);

            XmppServiceBroadcastEventEmitter.broadcastContactRenamed(remoteAccount, newAlias);

        } catch (Exception exc) {
            Logger.error(TAG, "Error while renaming contact: " + remoteAccount, exc);
        }
    }

    public void clearConversationsWith(String remoteAccount) {
        try {
            Logger.debug(TAG, "Clearing conversations with " + remoteAccount
                    + " for " + mAccount.getXmppJid());

            new MessagesProvider(mDatabase)
                    .deleteConversation(mAccount.getXmppJid(), remoteAccount)
                    .execute();

            XmppServiceBroadcastEventEmitter.broadcastConversationsCleared(remoteAccount);

        } catch (Exception exc) {
            Logger.error(TAG, "Error while clearing conversations with " + remoteAccount, exc);
            XmppServiceBroadcastEventEmitter.broadcastConversationsClearError(remoteAccount);
        }
    }

    // Connection listener implementation
    @Override
    public void connected(XMPPConnection connection) {
        Logger.debug(TAG, "connected");
        XmppService.setConnected(true);
    }

    @Override
    public void authenticated(XMPPConnection connection, boolean resumed) {
        if (resumed) {
            Logger.debug(TAG, "authenticated after resumption");
        } else {
            Logger.debug(TAG, "authenticated");
        }

        XmppService.setConnected(true);

        try {
            sendPendingMessages();
            Roster.getInstanceFor(mConnection).reload();
        } catch (Exception exc) {
            Logger.info(TAG, "Failed to automatically send pending messages on authentication");
        }
    }

    @Override
    public void connectionClosed() {
        Logger.debug(TAG, "connection closed");
        XmppService.setConnected(false);
    }

    @Override
    public void connectionClosedOnError(Exception e) {
        Logger.error(TAG, "connection closed on error", e);
        XmppService.setConnected(false);
    }

    @Override
    public void reconnectionSuccessful() {
        Logger.debug(TAG, "reconnection is successful");
        XmppService.setConnected(true);

        try {
            sendPendingMessages();
            Roster.getInstanceFor(mConnection).reload();
        } catch (Exception exc) {
            Logger.info(TAG, "Failed to automatically send pending messages after reconnection");
        }
    }

    @Override
    public void reconnectingIn(int seconds) {
        Logger.debug(TAG, "Reconnection will happen in " + seconds + "s");
        XmppService.setConnected(false);
    }

    @Override
    public void reconnectionFailed(Exception e) {
        Logger.error(TAG, "reconnection failed", e);
        XmppService.setConnected(false);
    }
    // Connection listener implementation

    @Override
    public void pingFailed() {
        Logger.info(TAG, "Ping to server failed!");
    }

    @Override
    public void chatCreated(Chat chat, boolean createdLocally) {
        Logger.debug(TAG, "chat created");
        chat.addMessageListener(this);
    }

    @Override
    public void processMessage(Chat chat, Message message) {
        if (message.getType().equals(Message.Type.chat) || message.getType().equals(Message.Type.normal)) {
            if (message.getBody() != null) {
                saveMessage(message.getFrom(), message.getBody(), true);
            }
        }
    }

    // start roster listener implementation
    @Override
    public void entriesAdded(Collection<String> addresses) {
        entriesUpdated(addresses);
    }

    @Override
    public void entriesUpdated(Collection<String> addresses) {
        if (addresses == null || addresses.isEmpty()) {
            return;
        }

        Roster roster = Roster.getInstanceFor(mConnection);
        if (roster == null) {
            Logger.info(TAG, "entriesUpdated - No roster instance, skipping rebuild roster");
            return;
        }

        ArrayList<XmppRosterEntry> entries = getRosterEntries();
        if (entries == null || entries.isEmpty()) {
            Logger.info(TAG, "entriesUpdated - No roster entries. Skipping rebuild roster");
            return;
        }

        for (String destination : addresses) {
            destination = getXmppJid(destination);
            RosterEntry entry = roster.getEntry(destination);
            XmppRosterEntry xmppRosterEntry = getRosterEntryFor(roster, entry);
            int index = entries.indexOf(xmppRosterEntry);
            if (index < 0) {
                entries.add(xmppRosterEntry);
            } else {
                entries.set(index, xmppRosterEntry);
            }

        }

        Collections.sort(entries);
        XmppServiceBroadcastEventEmitter.broadcastRosterChanged();
    }

    @Override
    public void entriesDeleted(Collection<String> addresses) {
        if (addresses == null || addresses.isEmpty()) {
            return;
        }

        for (String destination : addresses) {
            destination = getXmppJid(destination);
            sendUnsubscriptionRequestTo(destination);

            Roster roster = Roster.getInstanceFor(mConnection);
            if (roster == null) {
                Logger.info(TAG, "presenceChanged - No roster instance, skipping rebuild roster");
                return;
            }

            ArrayList<XmppRosterEntry> entries = getRosterEntries();
            if (entries == null || entries.isEmpty()) {
                Logger.info(TAG, "presenceChanged - No roster entries. Skipping rebuild roster");
                return;
            }

            int index = entries.indexOf(new XmppRosterEntry().setXmppJID(destination));
            if (index >= 0) {
                entries.remove(index);
            }
        }

        XmppServiceBroadcastEventEmitter.broadcastRosterChanged();
    }

    @Override
    public void presenceChanged(Presence presence) {
        Roster roster = Roster.getInstanceFor(mConnection);
        if (roster == null) {
            Logger.info(TAG, "presenceChanged - No roster instance, skipping rebuild roster");
            return;
        }

        ArrayList<XmppRosterEntry> entries = getRosterEntries();
        if (entries == null || entries.isEmpty()) {
            Logger.info(TAG, "presenceChanged - No roster entries. Skipping rebuild roster");
            return;
        }

        String from = getXmppJid(presence.getFrom());
        int index = entries.indexOf(new XmppRosterEntry().setXmppJID(from));

        if (index < 0) {
            Logger.info(TAG, "Presence from " + from + " which is not in the roster. Skipping rebuild roster");
            return;
        }

        Presence rosterPresence = roster.getPresence(from);
        entries.get(index)
               .setAvailable(rosterPresence.isAvailable())
               .setPresenceMode(rosterPresence.getMode().ordinal())
               .setPersonalMessage(rosterPresence.getStatus());

        Collections.sort(entries);
        XmppServiceBroadcastEventEmitter.broadcastRosterChanged();
    }
    // end roster listener implementation

    private String getXmppJid(String destination) {
        if (destination.contains("/")) {
            return destination.split("/")[0];
        }

        return destination;
    }

    private ArrayList<XmppRosterEntry> getRosterEntries() {
        ArrayList<XmppRosterEntry> entries = XmppService.getRosterEntries();
        if (entries == null || entries.isEmpty()) {
            rebuildRoster();
        }
        return XmppService.getRosterEntries();
    }

    private void rebuildRoster() {
        Roster roster = Roster.getInstanceFor(mConnection);
        if (roster == null) {
            Logger.info(TAG, "no roster, skipping rebuild roster");
            return;
        }

        Set<RosterEntry> entries = roster.getEntries();
        ArrayList<XmppRosterEntry> newRoster = new ArrayList<>(entries.size());

        for (RosterEntry entry : entries) {
            XmppRosterEntry newEntry = getRosterEntryFor(roster, entry);
            newRoster.add(newEntry);
        }

        Collections.sort(newRoster);
        XmppService.setRosterEntries(newRoster);
        XmppServiceBroadcastEventEmitter.broadcastRosterChanged();
    }

    private XmppRosterEntry getRosterEntryFor(Roster roster, RosterEntry entry) {
        XmppRosterEntry newEntry = new XmppRosterEntry();
        newEntry.setXmppJID(entry.getUser())
                .setAlias(entry.getName())
                .setAvatar(getCachedAvatar(entry.getUser()));

        if (newEntry.getAvatar() == null) {
            newEntry.setAvatar(getAvatarFor(entry.getUser()));
        }

        Presence presence = roster.getPresence(entry.getUser());
        newEntry.setAvailable(presence.isAvailable())
                .setPresenceMode(presence.getMode().ordinal())
                .setPersonalMessage(presence.getStatus());

        newEntry.setUnreadMessages(mMessagesProvider.countUnreadMessages(mAccount.getXmppJid(), entry.getUser()));

        return newEntry;
    }

    private void sendSubscriptionRequestTo(String destination) {
        Logger.debug(TAG, "Sending subscription request to " + destination);
        try {
            Presence subscribe = new Presence(Presence.Type.subscribe);
            subscribe.setTo(destination);
            mConnection.sendStanza(subscribe);
        } catch (Exception exc) {
            Logger.error(TAG, "Can't send subscription request", exc);
        }
    }

    private void sendUnsubscriptionRequestTo(String destination) {
        Logger.debug(TAG, "Sending un-subscription request to " + destination);
        try {
            Presence subscribe = new Presence(Presence.Type.unsubscribe);
            subscribe.setTo(destination);
            mConnection.sendStanza(subscribe);
        } catch (Exception exc) {
            Logger.error(TAG, "Can't send subscription request", exc);
        }
    }

    private byte[] getCachedAvatar(String xmppJID) {
        ArrayList<XmppRosterEntry> rosterEntries = XmppService.getRosterEntries();

        if (rosterEntries == null || rosterEntries.isEmpty())
            return null;

        XmppRosterEntry search = new XmppRosterEntry().setXmppJID(xmppJID);
        int index = rosterEntries.indexOf(search);

        return (index < 0 ? null : rosterEntries.get(index).getAvatar());
    }
}
