package tk.munditv.xmppservice;

public interface XmppServiceCallback {

    public void onConnected();
    public void onDisconnected();
    public void onRosterChanged();
    public void onMessageAdded(String remoteAccount, boolean incoming);
    public void onContactAdded(String remoteAccount);
    public void onContactRemoved(String remoteAccount);
    public void onContactRenamed(String remoteAccount, String newAlias);
    public void onContactAddError(String remoteAccount);
    public void onConversationsCleared(String remoteAccount);
    public void onConversationsClearError(String remoteAccount);
    public void onMessageSent(long messageId);
    public void onMessageDeleted(long messageId);
}
