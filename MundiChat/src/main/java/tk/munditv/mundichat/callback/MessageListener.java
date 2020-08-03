package tk.munditv.mundichat.callback;

public interface MessageListener {
    public void onMessageAdded(String remoteAccount, boolean incoming);
    public void onConversationsCleared(String remoteAccount);
    public void onConversationsClearError(String remoteAccount);
    public void onMessageSent(long messageId);
    public void onMessageDeleted(long messageId);
}
