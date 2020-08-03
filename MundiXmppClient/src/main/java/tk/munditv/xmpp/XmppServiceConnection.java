package tk.munditv.xmpp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.bosh.BOSHConfiguration;
import org.jivesoftware.smack.bosh.XMPPBOSHConnection;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.RosterListener;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.iqregister.AccountManager;
import org.jivesoftware.smackx.iqregister.packet.Registration;
import org.jivesoftware.smackx.ping.PingFailedListener;
import org.jivesoftware.smackx.ping.PingManager;
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager;
import org.jivesoftware.smackx.vcardtemp.VCardManager;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.EntityJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Localpart;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collector;

import tk.munditv.xmpp.database.SqLiteDatabase;
import tk.munditv.xmpp.database.TransactionBuilder;
import tk.munditv.xmpp.database.providers.MessagesProvider;
import tk.munditv.xmpp.database.tables.MessagesTable;

/**
 * Implementation of the XMPP Connection.
 * @author gotev (Aleksandar Gotev)
 */
public class XmppServiceConnection
        implements ConnectionListener, PingFailedListener, ChatMessageListener,
                   ChatManagerListener, RosterListener {

    private static final String TAG = "XmppServiceConnection";

    private XmppAccount mAccount;
    private byte[] mOwnAvatar;
    private SqLiteDatabase mDatabase;
    private AbstractXMPPConnection mConnection;
    private MessagesProvider mMessagesProvider;

    public XmppServiceConnection(XmppAccount account, SqLiteDatabase database) {
        mAccount = account;
        mDatabase = database;
        mMessagesProvider = new MessagesProvider(mDatabase);
    }

    public void connect()
            throws IOException, XMPPException, SmackException,
            InterruptedException, XmppStringprepException {
        if (mConnection == null) {
            createConnection();
        }

        if (!mConnection.isConnected()) {
            if(mAccount.getBOSHFlag()) {
                String http_string;
                if(mAccount.getHttps()) http_string = "https://";
                else http_string = "http://";
                Logger.debug(TAG, "Connecting to BOSH url " + http_string +
                        mAccount.getHost() + ":" + mAccount.getPort() + "/http-bind");
            } else {
                Logger.debug(TAG, "Connecting to " + mAccount.getHost() + ":" + mAccount.getPort());
            }
            mConnection.connect();

            Roster roster = Roster.getInstanceFor(mConnection);
            roster.removeRosterListener(this);
            roster.addRosterListener(this);
            roster.setSubscriptionMode(Roster.SubscriptionMode.accept_all);
            roster.setRosterLoadedAtLogin(true);
        }

        if (!mConnection.isAuthenticated()) {
            dologin(null);

            Logger.debug(TAG, "PingManager " + mAccount.getXmppJid());
            PingManager.setDefaultPingInterval(XmppService.DEFAULT_PING_INTERVAL);
            PingManager pingManager = PingManager.getInstanceFor(mConnection);
            pingManager.registerPingFailedListener(this);

            //getVCard(mAccount.getXmppJid().toString());
            /*
            Logger.debug(TAG, "ChatManager " + mAccount.getXmppJid());
            ChatManager chatManager = ChatManager.getInstanceFor(mConnection);
            chatManager.removeChatListener(this);
            chatManager.addChatListener(this);

            Logger.debug(TAG, "DeliveryReceiptManager " + mAccount.getXmppJid());
            DeliveryReceiptManager receipts = DeliveryReceiptManager.getInstanceFor(mConnection);
            receipts.setAutoReceiptMode(DeliveryReceiptManager.AutoReceiptMode.always);
            receipts.autoAddDeliveryReceiptRequests();
            */
        }

        mOwnAvatar = getAvatarFor(null);
    }

    public void disconnect() {
        mConnection.disconnect();
    }

    private void createConnection() throws XmppStringprepException {
        Logger.debug(TAG, "creating new connection to " +
                mAccount.getHost().toString() + ":" + mAccount.getPort());

        if( mAccount.getBOSHFlag() ) {

            BOSHConfiguration config = BOSHConfiguration.builder()
                    .setUseHttps(true)
                    .setSecurityMode(ConnectionConfiguration.SecurityMode.disabled)
                    .setXmppDomain(mAccount.getServiceName())
                    .setHost(mAccount.getHost())
                    .setPort(mAccount.getPort())
                    .enableDefaultDebugger()
                    .setFile("/http-bind")
                    .build();
            XMPPBOSHConnection mconnect = new XMPPBOSHConnection(config);
            if (false) {
                mconnect = new XMPPBOSHConnection(mAccount.getXmppJid().toString(),
                        mAccount.getPassword(), mAccount.getHttps(), mAccount.getHost(),
                        mAccount.getPort(), "/http-bind", mAccount.getServiceName());
            }
            Logger.debug(TAG, "account = " + mAccount.getXmppJid().toString());
            Logger.debug(TAG, "password = " + mAccount.getPassword());
            Logger.debug(TAG, "https = " + mAccount.getHttps());
            Logger.debug(TAG, "host = " + mAccount.getHost());
            Logger.debug(TAG, "port = " + mAccount.getPort());
            Logger.debug(TAG, "filePath = /http-bind");
            Logger.debug(TAG, "serviceName = " + mAccount.getServiceName());
            mconnect.addConnectionListener(this);
            mConnection = mconnect;
        } else {
            XMPPTCPConnectionConfiguration.Builder builder = XMPPTCPConnectionConfiguration.builder()
                    .setXmppDomain(mAccount.getServiceName())
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
            XMPPTCPConnection mconnect = new XMPPTCPConnection(builder.build());
            mconnect.setUseStreamManagement(XmppService.USE_STREAM_MANAGEMENT);
            mconnect.setUseStreamManagementResumption(XmppService.USE_STREAM_MANAGEMENT);
            mconnect.setPreferredResumptionTime(XmppService.STREAM_MANAGEMENT_RESUMPTION_TIME);
            mconnect.addConnectionListener(this);
            mConnection = mconnect;
        }
    }

    public void dologin(XmppAccount account) throws XmppStringprepException {
        if (account != null) mAccount = account;
        Logger.debug(TAG, "Authenticating " + mAccount.getXmppJid());
        mAccount.setResourceName("MundiXmppClient");
        Resourcepart resourcepart = Resourcepart.from(mAccount.getResourceName().toString());
        try {
            mConnection.login(mAccount.getXmppJid(), mAccount.getPassword(), resourcepart);
        } catch (XMPPException | SmackException | IOException | InterruptedException e) {
            XmppService.setAuthenticated(false);
            e.printStackTrace();
        }
    }

    public boolean register(XmppAccount account) throws XmppStringprepException {
        mAccount = account;
            try {
                Logger.debug(TAG, "register(" + mAccount.getXmppJid() + ", "
                        + mAccount.getPassword() + ")");
                if (mConnection == null)
                    return false;

                String str = mAccount.getXmppJid().toString();
                Localpart myaccount = Localpart.from(str);
                Logger.debug(TAG, "localpart = " + myaccount.toString());
                AccountManager.getInstance(mConnection).
                        sensitiveOperationOverInsecureConnection(true);
                AccountManager.getInstance(mConnection).
                        createAccount(myaccount, mAccount.getPassword());
            } catch (InterruptedException | XMPPException | SmackException | IOException e) {
                Logger.error(TAG, e.getMessage());
                mConnection.disconnect();
                return false;
            }
        Logger.debug(TAG, "register succeful!");
        return true;
    }

    public void singleEntryUpdated(Jid entry) {
        Logger.debug(TAG, "singleEntryUpdated");
        if (entry == null || entry.toString().isEmpty())
            return;

        List<Jid> entries = new ArrayList<>(1);
        entries.add(entry);
        entriesUpdated(entries);
    }

    private void entriesUpdated(List<String> entries) {
        Logger.debug(TAG, "entriesUpdated");
    }

    public void addMessageAndProcessPending(Jid destinationJID, String message)
            throws SmackException.NotConnectedException,
            InterruptedException, XmppStringprepException {
        Logger.debug(TAG, "addMessageAndProcessPending");
        saveMessage(destinationJID, message, false);
        sendPendingMessages();
    }

    private void saveMessage(Jid remoteAccount, String message, boolean incoming) {
        Logger.debug(TAG, "saveMessage");
        try {
            MessagesProvider messagesProvider = new MessagesProvider(mDatabase);
            TransactionBuilder insertTransaction;

            if (incoming) {
                insertTransaction = messagesProvider.addIncomingMessage(
                        mAccount.getXmppJid().toString(),
                        remoteAccount.toString(), message);
            } else {
                insertTransaction = messagesProvider.addOutgoingMessage(
                        mAccount.getXmppJid().toString(),
                        remoteAccount.toString(), message);
            }

            insertTransaction.execute();
            if (incoming) {
                singleEntryUpdated(remoteAccount);
            }
            XmppServiceBroadcastEventEmitter.
                    broadcastMessageAdded(remoteAccount.toString(), incoming);

        } catch (Exception exc) {
            Logger.error(TAG, "Error while saving " + (incoming ? "incoming" : "outgoing") +
                              " message " + (incoming ? "from " : "to ") + remoteAccount, exc);
        }
    }

    private void sendMessage(EntityJid destinationJID, String message)
            throws SmackException.NotConnectedException, InterruptedException {
        Logger.debug(TAG, "Sending message to " + destinationJID);
        Chat chat = ChatManager.getInstanceFor(mConnection).createChat(destinationJID, this);
        chat.sendMessage(message);
    }

    public void sendPendingMessages()
            throws SmackException.NotConnectedException, InterruptedException, XmppStringprepException {
        Logger.debug(TAG, "Sending pending messages for " + mAccount.getXmppJid());

        MessagesProvider messagesProvider = new MessagesProvider(mDatabase);

        List<tk.munditv.xmpp.database.models.Message> messages =
                messagesProvider.getPendingMessages(mAccount.getXmppJid().toString());
        if (messages == null || messages.isEmpty()) return;

        for (tk.munditv.xmpp.database.models.Message message : messages) {
            String account = message.getRemoteAccount();
            EntityJid jid = JidCreate.entityBareFrom(account);
            sendMessage(jid, message.getMessage());
            messagesProvider.updateMessageStatus(message.getId(), MessagesTable.Status.SENT)
                            .execute();

            XmppServiceBroadcastEventEmitter.broadcastMessageSent(message.getId());
        }
    }

    public void setPresence(int presenceMode, Jid personalMessage)
            throws SmackException.NotConnectedException,
            InterruptedException, XmppStringprepException {
        Logger.debug(TAG, "setPresence");
        mAccount.setPresenceMode(presenceMode);
        mAccount.setPersonalMessage(personalMessage.toString());
        setPresence();
    }

    public void setPresence()
            throws SmackException.NotConnectedException,
            InterruptedException, XmppStringprepException {
        Presence.Mode presMode;
        Logger.debug(TAG, "setPresence");

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

        mConnection.sendStanza(new Presence(Presence.Type.available,
                mAccount.getPersonalMessage().toString(),
                mAccount.getPriority(), presMode));
    }

    public void setMyVCard() throws XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException, XmppStringprepException {
        Logger.debug(TAG, "getVcard()");
        VCardManager manager = VCardManager.getInstanceFor(mConnection);
        //manager.saveVCard(XmppVCard.getVCard(mAccount.getXmppJid().toString()));
        return;
    }

    public void getVCard(String account) {
        Logger.debug(TAG, "getVcard()");
        VCardManager manager = VCardManager.getInstanceFor(mConnection);
        VCard vcard = null;

        try {
            vcard = manager.loadVCard();
        } catch (Exception e){
            e.printStackTrace();
        }
        Logger.debug(TAG, "getVCard() Nick Name = " + vcard.getNickName());
        //XmppVCard.setVCard(account, vcard);
        return;
    }

    public void setOwnAvatar(String filePath)
            throws SmackException.NotConnectedException, XMPPException.XMPPErrorException,
            SmackException.NoResponseException, InterruptedException {
        Logger.debug(TAG, "setOwnAvatar");

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

    private byte[] getAvatarFor(EntityBareJid remoteAccount) {
        Logger.debug(TAG, "getAvatarFor(\"" + remoteAccount + "\")");
        try {
            VCardManager manager = VCardManager.getInstanceFor(mConnection);

            byte[] data;
            VCard card;
            if (remoteAccount == null || remoteAccount.toString().isEmpty()) {
                card = manager.loadVCard();
                //XmppVCard.setVCard(mAccount.getXmppJid().toString(), card);

            } else {
                card = manager.loadVCard(remoteAccount);
                //XmppVCard.setVCard(remoteAccount.toString(), card);
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

    public void addContact(BareJid remoteAccount, String alias) {
        Logger.debug(TAG, "addContact");
        try {
            Roster.getInstanceFor(mConnection).createEntry(remoteAccount, alias, null);
            XmppServiceBroadcastEventEmitter.broadcastContactAdded(remoteAccount);

        } catch (Exception exc) {
            Logger.error(TAG, "Error while adding contact: " + remoteAccount, exc);
            XmppServiceBroadcastEventEmitter.broadcastContactAddError(remoteAccount);
        }
    }

    public void removeContact(BareJid remoteAccount) {
        Logger.debug(TAG, "removeContact");
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


    public void renameContact(BareJid remoteAccount, String newAlias) {
        Logger.debug(TAG, "renameContact");
        try {
            Roster roster = Roster.getInstanceFor(mConnection);
            RosterEntry entry = roster.getEntry(remoteAccount);
            entry.setName(newAlias);

            XmppServiceBroadcastEventEmitter.broadcastContactRenamed(remoteAccount, newAlias);

        } catch (Exception exc) {
            Logger.error(TAG, "Error while renaming contact: " + remoteAccount, exc);
        }
    }

    public void clearConversationsWith(BareJid remoteAccount) {
        Logger.debug(TAG, "clearConversationsWith");
        try {
            Logger.debug(TAG, "Clearing conversations with " + remoteAccount
                    + " for " + mAccount.getXmppJid());

            new MessagesProvider(mDatabase)
                    .deleteConversation(mAccount.getXmppJid().toString(), remoteAccount.toString())
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
        Logger.debug(TAG, "Listener function - connected()");
        XmppService.setConnected(true);
    }

    @Override
    public void authenticated(XMPPConnection connection, boolean resumed) {
        Logger.debug(TAG, "Listener function - authenticated()");
        if (resumed) {
            Logger.debug(TAG, "authenticated after resumption");
        } else {
            Logger.debug(TAG, "authenticated");
        }

        XmppService.setAuthenticated(true);

        try {
            Logger.debug(TAG, "ChatManager " + mAccount.getXmppJid());
            ChatManager chatManager = ChatManager.getInstanceFor(mConnection);
            chatManager.removeChatListener(this);
            chatManager.addChatListener(this);

            Logger.debug(TAG, "DeliveryReceiptManager " + mAccount.getXmppJid());
            DeliveryReceiptManager receipts = DeliveryReceiptManager.getInstanceFor(mConnection);
            receipts.setAutoReceiptMode(DeliveryReceiptManager.AutoReceiptMode.always);
            receipts.autoAddDeliveryReceiptRequests();

            sendPendingMessages();
            Roster.getInstanceFor(mConnection).reload();
        } catch (Exception exc) {
            Logger.error(TAG, "Failed to automatically send pending messages on authentication");
        }
    }

    @Override
    public void connectionClosed() {
        Logger.debug(TAG, "Listener function - connectionClosed()");
        XmppService.setConnected(false);
    }

    @Override
    public void connectionClosedOnError(Exception e) {
        Logger.debug(TAG, "Listener function - connectionClosedOnError()");
        Logger.error(TAG, "connection closed on error", e);
        XmppService.setConnected(false);
    }

    // Connection listener implementation
    @Override
    public void pingFailed() {
        Logger.error(TAG,
                "Listener function - pingFailed() : Ping to server failed!");
    }

    @Override
    public void chatCreated(Chat chat, boolean createdLocally) {
        Logger.debug(TAG, "Listener function - chatCreated()");
        chat.addMessageListener(this);
    }

    @Override
    public void processMessage(Chat chat, Message message) {
        Logger.debug(TAG, "Listener function - processMessage()");
        if (message.getType().equals(Message.Type.chat) || message.getType().equals(Message.Type.normal)) {
            if (message.getBody() != null) {
                saveMessage(message.getFrom(), message.getBody(), true);
            }
        }
    }

    // start roster listener implementation
    @Override
    public void entriesAdded(Collection<Jid> addresses) {
        Logger.debug(TAG, "Listener function - entriesAdded()");
        entriesUpdated(addresses);
    }

    @Override
    public void entriesUpdated(Collection<Jid> addresses) {
        Logger.debug(TAG, "Listener function - entriesUpdated()");
        if (addresses == null || addresses.isEmpty()) {
            return;
        }

        Roster roster = Roster.getInstanceFor(mConnection);
        if (roster == null) {
            Logger.debug(TAG, "entriesUpdated - No roster instance, skipping rebuild roster");
            return;
        }

        ArrayList<XmppRosterEntry> entries = null;
        try {
            entries = getRosterEntries();
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }
        if (entries == null || entries.isEmpty()) {
            Logger.debug(TAG, "entriesUpdated - No roster entries. Skipping rebuild roster");
            return;
        }

        for (Jid destination : addresses) {
            try {
                destination = JidCreate.entityFrom((CharSequence) getXmppJid(destination));
            } catch (XmppStringprepException e) {
                e.printStackTrace();
            }
            RosterEntry entry = roster.getEntry((BareJid) destination);
            XmppRosterEntry xmppRosterEntry = null;
            try {
                xmppRosterEntry = getRosterEntryFor(roster, entry);
            } catch (XmppStringprepException e) {
                e.printStackTrace();
            }
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
    public void entriesDeleted(Collection<Jid> addresses) {
        Logger.debug(TAG, "Listener function - entriesDeleted()");
        if (addresses == null || addresses.isEmpty()) {
            return;
        }

        for (Jid destination : addresses) {
            try {
                destination = JidCreate.entityFrom((CharSequence) getXmppJid(destination));
            } catch (XmppStringprepException e) {
                e.printStackTrace();
            }
            sendUnsubscriptionRequestTo(destination);

            Roster roster = Roster.getInstanceFor(mConnection);
            if (roster == null) {
                Logger.debug(TAG, "presenceChanged - No roster instance, skipping rebuild roster");
                return;
            }

            ArrayList<XmppRosterEntry> entries = null;
            try {
                entries = getRosterEntries();
            } catch (XmppStringprepException e) {
                e.printStackTrace();
            }
            if (entries == null || entries.isEmpty()) {
                Logger.debug(TAG, "presenceChanged - No roster entries. Skipping rebuild roster");
                return;
            }

            int index = entries.indexOf(new XmppRosterEntry().setXmppJID(destination.toString()));
            if (index >= 0) {
                entries.remove(index);
            }
        }

        XmppServiceBroadcastEventEmitter.broadcastRosterChanged();
    }

    @Override
    public void presenceChanged(Presence presence) {
        Logger.debug(TAG, "Listener function - presenceChanged()");
        Logger.debug(TAG, "presence mode = " + presence.getMode());
        Logger.debug(TAG, "presence String = " + presence.toString());
        Roster roster = Roster.getInstanceFor(mConnection);
        if (roster == null) {
            Logger.debug(TAG, "presenceChanged - No roster instance, skipping rebuild roster");
            return;
        }

        ArrayList<XmppRosterEntry> entries = null;
        try {
            entries = getRosterEntries();
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }
        if (entries == null || entries.isEmpty()) {
            Logger.debug(TAG, "presenceChanged - No roster entries. Skipping rebuild roster");
            return;
        }

        Jid from = null;
        try {
            from = JidCreate.entityFrom(getXmppJid(presence.getFrom()).toString());
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }
        int index = entries.indexOf(new XmppRosterEntry().setXmppJID(from.toString()));

        if (index < 0) {
            Logger.debug(TAG, "Presence from " + from
                    + " which is not in the roster. Skipping rebuild roster");
            return;
        }

        Presence rosterPresence = roster.getPresence((BareJid) from);
        entries.get(index)
               .setAvailable(rosterPresence.isAvailable())
               .setPresenceMode(rosterPresence.getMode().ordinal())
               .setPersonalMessage(rosterPresence.getStatus());

        Logger.debug(TAG, "entries(" + index + ") = "
                + entries.get(index).toString());
        Collections.sort(entries);
        XmppServiceBroadcastEventEmitter.broadcastRosterChanged();
    }
    // end roster listener implementation

    private Serializable getXmppJid(Jid destination) {
        Logger.debug(TAG, "getXmppJid(\"" + destination.toString() + "\")");
        if (destination.toString().contains("/")) {
            Logger.debug(TAG, "Serializable string = "
                    + destination.toString().split("/")[0]);
            return destination.toString().split("/")[0];
        }

        return destination;
    }

    public ArrayList<XmppRosterEntry> getRosterEntries() throws XmppStringprepException {
        Logger.debug(TAG, "getRosterEntries");
        ArrayList<XmppRosterEntry> entries = XmppService.getRosterEntries();
        if (entries == null || entries.isEmpty()) {
            rebuildRoster();
        }
        return XmppService.getRosterEntries();
    }

    public void rebuildRoster() throws XmppStringprepException {
        Logger.debug(TAG, "rebuildRoster");
        Roster roster = Roster.getInstanceFor(mConnection);
        if (roster == null) {
            Logger.debug(TAG, "no roster, skipping rebuild roster");
            return;
        }

        Set<RosterEntry> entries = roster.getEntries();
        ArrayList<XmppRosterEntry> newRoster = new ArrayList<>(entries.size());

        for (RosterEntry entry : entries) {
            XmppRosterEntry newEntry = getRosterEntryFor(roster, entry);
            String jid = newEntry.getXmppJID();
            String alias = newEntry.getAlias();
            Logger.debug(TAG, "get XmppRosterEntry --- Jabber ID = " + jid
                    + ", alias = " + alias);
            newRoster.add(newEntry);
        }

        Collections.sort(newRoster);
        XmppService.setRosterEntries(newRoster);
        XmppServiceBroadcastEventEmitter.broadcastRosterChanged();
    }

    public XmppRosterEntry getRosterEntryFor(Roster roster, RosterEntry entry)
            throws XmppStringprepException {
        Logger.debug(TAG, "getRosterEntryFor");
        XmppRosterEntry newEntry = new XmppRosterEntry();

        newEntry.setXmppJID(entry.getJid().toString())
                .setAlias(entry.getJid().toString())
                .setAvatar(getCachedAvatar(entry.getJid()));

        if (newEntry.getAvatar() == null) {
            EntityBareJid jid = (EntityBareJid) entry.getJid();
            newEntry.setAvatar(getAvatarFor(jid));
        }

        Presence presence = roster.getPresence(entry.getJid());
        newEntry.setAvailable(presence.isAvailable())
                .setPresenceMode(presence.getMode().ordinal())
                .setPersonalMessage(presence.getStatus());

        newEntry.setUnreadMessages(mMessagesProvider.countUnreadMessages(mAccount.getXmppJid().toString(),
                entry.getName()));

        return newEntry;
    }

    public void sendUnsubscriptionRequestTo(Jid destination) {
        Logger.debug(TAG, "Sending un-subscription request to " + destination);
        try {
            Presence subscribe = new Presence(Presence.Type.unsubscribe);
            subscribe.setTo(destination);
            mConnection.sendStanza(subscribe);
        } catch (Exception exc) {
            Logger.error(TAG, "Can't send subscription request", exc);
        }
    }

    private byte[] getCachedAvatar(Jid xmppJID) {
        Logger.debug(TAG, "getCachedAvatar");

        ArrayList<XmppRosterEntry> rosterEntries = XmppService.getRosterEntries();

        if (rosterEntries == null || rosterEntries.isEmpty())
            return null;

        XmppRosterEntry search = new XmppRosterEntry().setXmppJID(xmppJID.toString());
        int index = rosterEntries.indexOf(search);

        return (index < 0 ? null : rosterEntries.get(index).getAvatar());
    }


}
