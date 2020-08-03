package tw.com.munditv.mundicontroller.xmppservice;

import android.graphics.drawable.Drawable;
import android.util.Log;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.bosh.XMPPBOSHConnection;
import org.jivesoftware.smack.chat.ChatManagerListener;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.RosterGroup;
import org.jivesoftware.smack.roster.RosterListener;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.filetransfer.OutgoingFileTransfer;
import org.jivesoftware.smackx.iqregister.AccountManager;
import org.jivesoftware.smackx.muc.HostedRoom;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.MultiUserChatManager;
import org.jivesoftware.smackx.offline.OfflineMessageManager;
import org.jivesoftware.smackx.ping.PingFailedListener;
import org.jivesoftware.smackx.search.ReportedData;
import org.jivesoftware.smackx.search.UserSearchManager;
import org.jivesoftware.smackx.vcardtemp.VCardManager;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;
import org.jivesoftware.smackx.xdata.Form;
import org.jivesoftware.smackx.xdata.FormField;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Localpart;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XmppConnection
        implements ConnectionListener, PingFailedListener, ChatMessageListener,
        ChatManagerListener, RosterListener {

    private final static String TAG = "XmppConnection";

    private int SERVER_PORT = Constant.OPENFIRE_PORT;
    private String SERVER_HOST = Constant.OPENFIRE_IP;
    private String SERVER_NAME = Constant.OPENFIRE_NAME;
    private AbstractXMPPConnection connection = null;
    private static XmppConnection xmppConnection = new XmppConnection();
    private XMConnectionListener connectionListener;

    private boolean isBOSH = true;

    // For BOSH PROTOCOL only (jitsi-meet prosody server used
    private String mHost = Constant.WEBRTC_URL;
    private String mFilePath = Constant.WEBRTC_FILEPATH;
    private int mPort = Constant.WEBRTC_PORT;
    private boolean isHttps = Constant.WEBRTC_HTTPS;

    /**
     * 單例模式
     *
     * @return XmppConnection
     */
    public synchronized static XmppConnection getInstance() {
        Log.d(TAG, "getInstance()");
        return xmppConnection;
    }

    @Override
    public void connected(XMPPConnection connection) {

    }

    @Override
    public void authenticated(XMPPConnection connection, boolean resumed) {

    }

    @Override
    public void connectionClosed() {

    }

    @Override
    public void connectionClosedOnError(Exception e) {

    }

    @Override
    public void chatCreated(org.jivesoftware.smack.chat.Chat chat, boolean createdLocally) {

    }

    @Override
    public void processMessage(org.jivesoftware.smack.chat.Chat chat, Message message) {

    }

    @Override
    public void entriesAdded(Collection<Jid> addresses) {

    }

    @Override
    public void entriesUpdated(Collection<Jid> addresses) {

    }

    @Override
    public void entriesDeleted(Collection<Jid> addresses) {

    }

    @Override
    public void presenceChanged(Presence presence) {

    }

    @Override
    public void pingFailed() {

    }

    /**
     * 建立連線
     */
    public AbstractXMPPConnection getConnection() {
        Log.d(TAG, "getConnection()");
        if (connection == null) {
            // 開執行緒開啟連線，避免在主執行緒裡面執行HTTP請求
            // Caused by: android.os.NetworkOnMainThreadException
            new Thread(new Runnable() {
                @Override
                public void run() {
                    openConnection();
                }
            }).start();
        }
        return connection;
    }

    /**
     * 判斷是否已連線
     */
    public boolean checkConnection() {
        Log.d(TAG, "checkConnection()");
        return null != connection && connection.isConnected();
    }

    /**
     * 開啟連線
     */
    public boolean openConnection() {
        Log.d(TAG, "openConnection()");
        try {
            if (null == connection || !connection.isAuthenticated()) {
                if(isBOSH) {
                    DomainBareJid mXmppServiceDomain = JidCreate.domainBareFrom(mHost);
                    connection = new XMPPBOSHConnection(null, null,
                            isHttps, mHost, mPort, mFilePath,mXmppServiceDomain);
                } else {
                    SmackConfiguration.DEBUG = true;
                    XMPPTCPConnectionConfiguration.Builder config =
                            XMPPTCPConnectionConfiguration.builder();
                    //設定openfire主機IP
                    config.setHostAddress(InetAddress.getByName(SERVER_HOST));
                    //設定openfire伺服器名稱
                    config.setXmppDomain(SERVER_NAME);
                    //設定埠號：預設5222
                    config.setPort(SERVER_PORT);
                    //禁用SSL連線
                    config.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled).
                            setCompressionEnabled(false);
                    //設定Debug
                    //config.setDebuggerFactory(DEBUG);
                    //設定離線狀態
                    config.setSendPresence(false);
                    //設定開啟壓縮，可以節省流量
                    config.setCompressionEnabled(true);

                    //需要經過同意才可以新增好友
                    Roster.setDefaultSubscriptionMode(Roster.SubscriptionMode.manual);

                    // 將相應機制隱掉
                    //SASLAuthentication.blacklistSASLMechanism("SCRAM-SHA-1");
                    //SASLAuthentication.blacklistSASLMechanism("DIGEST-MD5");

                    connection = new XMPPTCPConnection(config.build());
                }
                connection.connect();// 連線到伺服器
                return true;
            }
        } catch (XMPPException | SmackException
                | IOException | InterruptedException xe) {
            xe.printStackTrace();
            connection = null;
        }
        return false;
    }

    /**
     * 關閉連線
     */
    public void closeConnection() {
        Log.d(TAG, "closeConnection()");
        if (connection != null) {
            // 移除連線監聽
            connection.removeConnectionListener(connectionListener);
            if (connection.isConnected())
                connection.disconnect();
            connection = null;
        }
        Log.i("XmppConnection", "關閉連線");
    }

    /**
     * 判斷連線是否通過了身份驗證
     * 即是否已登入
     *
     * @return
     */
    public boolean isAuthenticated() {
        Log.d(TAG, "isAuthenticated()");
        return connection != null && connection.isConnected()
                && connection.isAuthenticated();
    }

    /**
     * 登入
     *
     * @param account  登入帳號
     * @param password 登入密碼
     * @return true登入成功
     */
    public boolean login(String account, String password) {
        Log.d(TAG, "login(" + account +", " + password + ")");
        try {
            if (getConnection() == null)
                return false;

            getConnection().login(account, password);

            // 更改線上狀態
            setPresence(0);

            // 新增連線監聽
            connectionListener = new XMConnectionListener(account, password);
            getConnection().addConnectionListener(connectionListener);
            return true;
        } catch (XMPPException | IOException
                | SmackException | InterruptedException xe) {
            xe.printStackTrace();
        }
        return false;
    }

    /**
     * 註冊
     *
     * @param account  註冊帳號
     * @param password 註冊密碼
     * @return 1、註冊成功 0、註冊失敗
     */
    public String register(String account, String password) {
        Log.d(TAG, "register(" + account +", " + password + ")");
        if (getConnection() == null)
            return "0";
        try {
            AccountManager.getInstance(connection).
                    createAccount(Localpart.from(account), password);
        } catch (XmppStringprepException | InterruptedException
                | XMPPException | SmackException e) {
            e.printStackTrace();
            return "0";
        }

        return "1";
    }

    /**
     * 更改使用者狀態
     */
    public void setPresence(int code) {
        Log.d(TAG, "setPresence(), code = " + code);
        XMPPConnection con = getConnection();
        if (con == null)
            return;
        Presence presence;
        try {
            switch (code) {
                case 0:
                    presence = new Presence(Presence.Type.available);
                    con.sendStanza(presence);
                    Log.v("state", "設定線上");
                    break;
                case 1:
                    presence = new Presence(Presence.Type.available);
                    presence.setMode(Presence.Mode.chat);
                    con.sendStanza(presence);
                    Log.v("state", "設定Q我吧");
                    break;
                case 2:
                    presence = new Presence(Presence.Type.available);
                    presence.setMode(Presence.Mode.dnd);
                    con.sendStanza(presence);
                    Log.v("state", "設定忙碌");
                    break;
                case 3:
                    presence = new Presence(Presence.Type.available);
                    presence.setMode(Presence.Mode.away);
                    con.sendStanza(presence);
                    Log.v("state", "設定離開");
                    break;
                case 4:
//                    Roster roster = con.getRoster();
//                    Collection<RosterEntry> entries = roster.getEntries();
//                    for (RosterEntry entry : entries) {
//                        presence = new Presence(Presence.Type.unavailable);
//                        presence.setPacketID(Packet.ID_NOT_AVAILABLE);
//                        presence.setFrom(con.getUser());
//                        presence.setTo(entry.getUser());
//                        con.sendPacket(presence);
//                        Log.v("state", presence.toXML());
//                    }
//                    // 向同一使用者的其他客戶端傳送隱身狀態
//                    presence = new Presence(Presence.Type.unavailable);
//                    presence.setPacketID(Packet.ID_NOT_AVAILABLE);
//                    presence.setFrom(con.getUser());
//                    presence.setTo(StringUtils.parseBareAddress(con.getUser()));
//                    con.sendStanza(presence);
//                    Log.v("state", "設定隱身");
//                    break;
                case 5:
                    presence = new Presence(Presence.Type.unavailable);
                    con.sendStanza(presence);
                    Log.v("state", "設定離線");
                    break;
                default:
                    break;
            }
        } catch (SmackException.NotConnectedException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 獲取所有組
     *
     * @return 所有組集合
     */
    public List<RosterGroup> getGroups() {
        Log.d(TAG, "getGroups()");
        if (getConnection() == null)
            return null;
        List<RosterGroup> groupList = new ArrayList<>();
        Collection<RosterGroup> rosterGroup = Roster.getInstanceFor(connection).getGroups();
        for (RosterGroup aRosterGroup : rosterGroup) {
            groupList.add(aRosterGroup);
        }
        return groupList;
    }

    /**
     * 獲取某個組裡面的所有好友
     *
     * @param groupName 組名
     * @return List<RosterEntry>
     */
    public List<RosterEntry> getEntriesByGroup(String groupName) {
        Log.d(TAG, "getEntriesByGroup() groupName = " + groupName);
        if (getConnection() == null)
            return null;
        List<RosterEntry> EntriesList = new ArrayList<>();
        RosterGroup rosterGroup = Roster.getInstanceFor(connection).getGroup(groupName);
        Collection<RosterEntry> rosterEntry = rosterGroup.getEntries();
        for (RosterEntry aRosterEntry : rosterEntry) {
            EntriesList.add(aRosterEntry);
        }
        return EntriesList;
    }

    /**
     * 獲取所有好友資訊
     *
     * @return List<RosterEntry>
     */
    public List<RosterEntry> getAllEntries() {
        Log.d(TAG, "getAllEntries()");
        if (getConnection() == null)
            return null;
        List<RosterEntry> Enlist = new ArrayList<>();
        Collection<RosterEntry> rosterEntry = Roster.
                getInstanceFor(connection).getEntries();
        for (RosterEntry aRosterEntry : rosterEntry) {
            Enlist.add(aRosterEntry);
        }
        return Enlist;
    }

    /**
     * 獲取使用者VCard資訊
     *
     * @param user user
     * @return VCard
     */
    public VCard getUserVCard(String user) {
        Log.d(TAG, "getUserVCard() user = " + user);

        if (getConnection() == null)
            return null;
        VCard vcard = new VCard();
        try {
            vcard = VCardManager.getInstanceFor(getConnection()).
                    loadVCard(JidCreate.entityBareFrom(user));
        } catch (XmppStringprepException | SmackException
                | InterruptedException | XMPPException.XMPPErrorException e) {
            e.printStackTrace();
        }

        return vcard;
    }

    /**
     * 獲取使用者頭像資訊
     *
     * @param user user
     * @return Drawable
     */
    public Drawable getUserImage(String user) {
        Log.d(TAG, "getUserImage() user = " + user);
        if (getConnection() == null)
            return null;
        ByteArrayInputStream bais = null;
        try {
            VCard vcard = new VCard();
            // 加入這句程式碼，解決No VCard for
            ProviderManager.addIQProvider("vCard", "vcard-temp",
                    new org.jivesoftware.smackx.vcardtemp.provider.VCardProvider());
            if (user == null || user.equals("") || user.trim().length() <= 0) {
                return null;
            }
            try {
                VCardManager.getInstanceFor(getConnection()).loadVCard(
                        JidCreate.entityBareFrom(user + "@"
                                + getConnection().getConfiguration().getServiceName()));
            } catch (XmppStringprepException | SmackException
                    | InterruptedException | XMPPException.XMPPErrorException e) {
                e.printStackTrace();
            }

            if (vcard.getAvatar() == null)
                return null;
            bais = new ByteArrayInputStream(vcard.getAvatar());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return FormatTools.getInstance().InputStream2Drawable(bais);
    }

    /**
     * 新增一個分組
     *
     * @param groupName groupName
     * @return boolean
     */
    public boolean addGroup(String groupName) {
        Log.d(TAG, "addGroup() groupName = " + groupName);

        if (getConnection() == null)
            return false;
        try {
            Roster.getInstanceFor(connection).createGroup(groupName);
            Log.v("addGroup", groupName + "建立成功");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 刪除分組
     *
     * @param groupName groupName
     * @return boolean
     */
    public boolean removeGroup(String groupName) {
        Log.d(TAG, "addGroup() removeGroup = " + groupName);
        return true;
    }

    /**
     * 新增好友 無分組
     *
     * @param userName userName
     * @param name     name
     * @return boolean
     */
    public boolean addUser(String userName, String name) {
        Log.d(TAG, "addUser() user = " + userName + ", name = " + name);

        if (getConnection() == null)
            return false;
        try {
            Roster.getInstanceFor(connection).
                    createEntry(JidCreate.entityBareFrom(userName), name, null);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 新增好友 有分組
     *
     * @param userName  userName
     * @param name      name
     * @param groupName groupName
     * @return boolean
     */
    public boolean addUser(String userName, String name, String groupName) {
        Log.d(TAG, "addUser() user = " + userName + ", name = "
                + name + ", groupName = " + groupName);

        if (getConnection() == null)
            return false;
        try {
            Presence subscription = new Presence(Presence.Type.subscribed);
            subscription.setTo(JidCreate.entityBareFrom(userName));
            userName += "@" + getConnection().getConfiguration().getServiceName();
            getConnection().sendStanza(subscription);
            Roster.getInstanceFor(connection).
                    createEntry(JidCreate.entityBareFrom(userName),
                            name, new String[]{groupName});
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 刪除好友
     *
     * @param userName userName
     * @return boolean
     */
    public boolean removeUser(String userName) {
        Log.d(TAG, "removeUser() userName = " + userName);
        if (getConnection() == null)
            return false;
        try {
            RosterEntry entry = null;
            if (userName.contains("@"))
                entry = Roster.getInstanceFor(connection).
                        getEntry(JidCreate.entityBareFrom(userName));
            else
                entry = Roster.getInstanceFor(connection).getEntry(JidCreate.entityBareFrom(
                        userName + "@" + getConnection().
                                getConfiguration().getServiceName()));
            if (entry == null)
                entry = Roster.getInstanceFor(connection).
                        getEntry(JidCreate.entityBareFrom(userName));
            Roster.getInstanceFor(connection).removeEntry(entry);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 查詢使用者
     *
     * @param userName userName
     * @return List<HashMap<String, String>>
     */
    public List<HashMap<String, String>> searchUsers(String userName) {
        Log.d(TAG, "searchUsers() userName = " + userName);
        if (getConnection() == null)
            return null;
        HashMap<String, String> user;
        List<HashMap<String, String>> results = new ArrayList<>();
        try {
            UserSearchManager usm = new UserSearchManager(getConnection());

            Form searchForm = usm.getSearchForm(getConnection().
                    getConfiguration().getServiceName());
            if (searchForm == null)
                return null;

            Form answerForm = searchForm.createAnswerForm();
            answerForm.setAnswer("userAccount", true);
            answerForm.setAnswer("userPhote", userName);
            ReportedData data = usm.getSearchResults(answerForm,
                    JidCreate.domainBareFrom("search"
                            + getConnection().getConfiguration().getServiceName()));

            List<ReportedData.Row> rowList = data.getRows();
            for (ReportedData.Row row : rowList) {
                user = new HashMap<>();
                user.put("userAccount", row.getValues("userAccount").toString());
                user.put("userPhote", row.getValues("userPhote").toString());
                results.add(user);
                // 若存在，則有返回,UserName一定非空，其他兩個若是有設，一定非空
            }
        } catch (SmackException | InterruptedException
                | XmppStringprepException | XMPPException e) {
            e.printStackTrace();
        }
        return results;
    }

    /**
     * 修改心情
     *
     * @param status
     */
    public void changeStateMessage(String status) {
        Log.d(TAG, "changeStateMessage() status = " + status);
        if (getConnection() == null)
            return;
        Presence presence = new Presence(Presence.Type.available);
        presence.setStatus(status);
        try {
            getConnection().sendStanza(presence);
        } catch (SmackException.NotConnectedException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 修改使用者頭像
     *
     * @param file file
     */
    public boolean changeImage(File file) {
        Log.d(TAG, "changeImage() file = " + file);

        if (getConnection() == null)
            return false;
        try {
            VCard vcard = new VCard();
            vcard.load(getConnection());

            byte[] bytes;

            bytes = getFileBytes(file);
            String encodedImage = StringUtils.encodeHex(bytes);
            vcard.setAvatar(bytes, encodedImage);
            vcard.setEncodedImage(encodedImage);
            vcard.setField("PHOTO", "<TYPE>image/jpg</TYPE><BINVAL>"
                    + encodedImage + "</BINVAL>", true);

            ByteArrayInputStream bais = new ByteArrayInputStream(
                    vcard.getAvatar());
            FormatTools.getInstance().InputStream2Bitmap(bais);

            vcard.save(getConnection());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 檔案轉位元組
     *
     * @param file file
     * @return byte[]
     * @throws IOException
     */
    private byte[] getFileBytes(File file) throws IOException {
        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(new FileInputStream(file));
            int bytes = (int) file.length();
            byte[] buffer = new byte[bytes];
            int readBytes = bis.read(buffer);
            if (readBytes != buffer.length) {
                throw new IOException("Entire file not read");
            }
            return buffer;
        } finally {
            if (bis != null) {
                bis.close();
            }
        }
    }

    /**
     * 刪除當前使用者
     *
     * @return true成功
     */
    public boolean deleteAccount() {
        Log.d(TAG, "deleteAccount()");
        if (getConnection() == null)
            return false;
        try {
            AccountManager.getInstance(connection).deleteAccount();
            return true;
        } catch (XMPPException | SmackException | InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 修改密碼
     *
     * @return true成功
     */
    public boolean changePassword(String pwd) {
        Log.d(TAG, "changePassword() password = " + pwd);

        if (getConnection() == null)
            return false;
        try {
            AccountManager.getInstance(connection).changePassword(pwd);
            return true;
        } catch (SmackException | InterruptedException | XMPPException.XMPPErrorException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 初始化會議室列表
     */
    public List<HostedRoom> getHostRooms() {
        Log.d(TAG, "getHostRooms()");
        if (getConnection() == null)
            return null;
        Collection<HostedRoom> hostrooms;
        List<HostedRoom> roominfos = new ArrayList<>();
        try {
            hostrooms = MultiUserChatManager.getInstanceFor(getConnection()).getHostedRooms(
                    JidCreate.domainBareFrom(getConnection().getConfiguration().getServiceName()));
            for (HostedRoom entry : hostrooms) {
                roominfos.add(entry);
                Log.i("room", "名字：" + entry.getName() + " - ID:" + entry.getJid());
            }
            Log.i("room", "服務會議數量:" + roominfos.size());
        } catch (XMPPException | XmppStringprepException | InterruptedException | SmackException e) {
            e.printStackTrace();
            return null;
        }
        return roominfos;
    }

    /**
     * 建立房間
     *
     * @param roomName 房間名稱
     */
    public MultiUserChat createRoom(String roomName, String password) {
        Log.d(TAG, "createRoom() roomName = " + roomName + ", password = " + password);

        if (getConnection() == null)
            return null;

        MultiUserChat muc = null;
        try {
            // 建立一個MultiUserChat
            muc = MultiUserChatManager.getInstanceFor(getConnection()).getMultiUserChat(
                    JidCreate.entityBareFrom(roomName + "@conference."
                            + getConnection().getConfiguration().getServiceName()));
            // 建立聊天室
            muc.create(Resourcepart.from(roomName));
            // 獲得聊天室的配置表單
            Form form = muc.getConfigurationForm();
            // 根據原始表單建立一個要提交的新表單。
            Form submitForm = form.createAnswerForm();
            // 向要提交的表單新增預設答覆
            for (FormField formField : form.getFields()) {
                if (FormField.Type.hidden == formField.getType()
                        && formField.getVariable() != null) {
                    // 設定預設值作為答覆
                    submitForm.setDefaultAnswer(formField.getVariable());
                }
            }
            // 設定聊天室的新擁有者
            List<String> owners = new ArrayList<>();
            owners.add(getConnection().getUser().asEntityBareJidString());// 使用者JID
            submitForm.setAnswer("muc#roomconfig_roomowners", owners);
            // 設定聊天室是持久聊天室，即將要被儲存下來
            submitForm.setAnswer("muc#roomconfig_persistentroom", true);
            // 房間僅對成員開放
            submitForm.setAnswer("muc#roomconfig_membersonly", false);
            // 允許佔有者邀請其他人
            submitForm.setAnswer("muc#roomconfig_allowinvites", true);
            if (!password.equals("")) {
                // 進入是否需要密碼
                submitForm.setAnswer("muc#roomconfig_passwordprotectedroom",
                        true);
                // 設定進入密碼
                submitForm.setAnswer("muc#roomconfig_roomsecret", password);
            }
            // 能夠發現佔有者真實 JID 的角色
            // submitForm.setAnswer("muc#roomconfig_whois", "anyone");
            // 登入房間對話
            submitForm.setAnswer("muc#roomconfig_enablelogging", true);
            // 僅允許註冊的暱稱登入
            submitForm.setAnswer("x-muc#roomconfig_reservednick", true);
            // 允許使用者修改暱稱
            submitForm.setAnswer("x-muc#roomconfig_canchangenick", false);
            // 允許使用者註冊房間
            submitForm.setAnswer("x-muc#roomconfig_registration", false);
            // 傳送已完成的表單（有預設值）到伺服器來配置聊天室
            muc.sendConfigurationForm(submitForm);
        } catch (XMPPException | XmppStringprepException
                | SmackException | InterruptedException e) {
            e.printStackTrace();
            return null;
        }
        return muc;
    }

    /**
     * 加入會議室
     *
     * @param user      暱稱
     * @param roomsName 會議室名
     */
    public MultiUserChat joinMultiUserChat(String user, String roomsName) {
        Log.d(TAG, "joinMultiUserChat() user = " + user + ", roomsName = " + roomsName);

        if (getConnection() == null)
            return null;
        try {
            // 使用XMPPConnection建立一個MultiUserChat視窗
            MultiUserChat muc = MultiUserChatManager.
                    getInstanceFor(getConnection()).getMultiUserChat(
                    JidCreate.entityBareFrom(roomsName + "@conference."
                            + getConnection().getConfiguration().getServiceName()));

            // 使用者加入聊天室
            muc.join(Resourcepart.from(user));

            Log.i("MultiUserChat", "會議室【" + roomsName + "】加入成功........");
            return muc;
        } catch (XMPPException | XmppStringprepException
                | InterruptedException | SmackException e) {
            e.printStackTrace();
            Log.i("MultiUserChat", "會議室【" + roomsName + "】加入失敗........");
            return null;
        }
    }

    /**
     * 傳送群組聊天訊息
     *
     * @param muc     muc
     * @param message 訊息文字
     */
    public void sendGroupMessage(MultiUserChat muc, String message) {
        try {
            muc.sendMessage(message);
        } catch (SmackException.NotConnectedException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 查詢會議室成員名字
     *
     * @param muc
     */
    public List<String> findMulitUser(MultiUserChat muc) {
        if (getConnection() == null)
            return null;
        List<String> listUser = new ArrayList<>();
        List<EntityFullJid> it = muc.getOccupants();
        // 遍歷出聊天室人員名稱
        for (EntityFullJid entityFullJid : it) {
            // 聊天室成員名字
            String name = entityFullJid.toString();
            listUser.add(name);
        }
        return listUser;
    }

    /**
     * 建立聊天視窗
     *
     * @param JID JID
     * @return Chat
     */
    public Chat getFriendChat(String JID) {
        try {
            return ChatManager.getInstanceFor(XmppConnection.getInstance().getConnection())
                    .chatWith(JidCreate.entityBareFrom(JID));
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 傳送單人聊天訊息
     *
     * @param chat    chat
     * @param message 訊息文字
     */
    public void sendSingleMessage(Chat chat, String message) {
        try {
            chat.send(message);
        } catch (SmackException.NotConnectedException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 發訊息
     *
     * @param chat    chat
     * @param muc     muc
     * @param message message
     */
    public void sendMessage(Chat chat, MultiUserChat muc, String message) {
        if (chat != null) {
            sendSingleMessage(chat, message);
        } else if (muc != null) {
            sendGroupMessage(muc, message);
        }
    }

    /**
     * 傳送檔案
     *
     * @param user
     * @param filePath
     */
    public void sendFile(String user, String filePath) {
        if (getConnection() == null)
            return;
        // 建立檔案傳輸管理器
        FileTransferManager manager = FileTransferManager.getInstanceFor(getConnection());

        // 建立輸出的檔案傳輸
        OutgoingFileTransfer transfer = null;
        try {
            transfer = manager.createOutgoingFileTransfer(JidCreate.entityFullFrom(user));
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }

        // 傳送檔案
        try {
            if (transfer != null)
                transfer.sendFile(new File(filePath), "You won't believe this!");
        } catch (SmackException e) {
            e.printStackTrace();
        }
    }

    /**
     * 獲取離線訊息
     *
     * @return
     */
    public Map<String, List<HashMap<String, String>>> getHisMessage() {
        if (getConnection() == null)
            return null;
        Map<String, List<HashMap<String, String>>> offlineMsgs = null;

        try {
            OfflineMessageManager offlineManager = new OfflineMessageManager(getConnection());
            List<Message> messageList = offlineManager.getMessages();

            int count = offlineManager.getMessageCount();
            if (count <= 0)
                return null;
            offlineMsgs = new HashMap<>();

            for (Message message : messageList) {
                String fromUser = message.getFrom().toString();
                HashMap<String, String> history = new HashMap<>();
                history.put("useraccount", getConnection().getUser().asEntityBareJidString());
                history.put("friendaccount", fromUser);
                history.put("info", message.getBody());
                history.put("type", "left");
                if (offlineMsgs.containsKey(fromUser)) {
                    offlineMsgs.get(fromUser).add(history);
                } else {
                    List<HashMap<String, String>> temp = new ArrayList<HashMap<String, String>>();
                    temp.add(history);
                    offlineMsgs.put(fromUser, temp);
                }
            }
            offlineManager.deleteMessages();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return offlineMsgs;
    }

    /**
     * 判斷OpenFire使用者的狀態 strUrl :
     * url格式 - http://my.openfire.com:9090/plugins/presence
     * /[email protected]_NAME&type=xml
     * 返回值 : 0 - 使用者不存在; 1 - 使用者線上; 2 - 使用者離線
     * 說明 ：必須要求 OpenFire載入 presence 外掛，同時設定任何人都可以訪問
     */
    public int IsUserOnLine(String user) {
        String url = "http://" + SERVER_HOST + ":9090/plugins/presence/status?" +
                "jid=" + user + "@" + SERVER_NAME + "&type=xml";
        int shOnLineState = 0; // 不存在
        try {
            URL oUrl = new URL(url);
            URLConnection oConn = oUrl.openConnection();
            if (oConn != null) {
                BufferedReader oIn = new BufferedReader(new InputStreamReader(
                        oConn.getInputStream()));
                String strFlag = oIn.readLine();
                oIn.close();
                System.out.println("strFlag" + strFlag);
                if (strFlag.contains("type=\"unavailable\"")) {
                    shOnLineState = 2;
                }
                if (strFlag.contains("type=\"error\"")) {
                    shOnLineState = 0;
                } else if (strFlag.contains("priority") || strFlag.contains("id=\"")) {
                    shOnLineState = 1;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return shOnLineState;
    }
}
