// IChatServiceInterface.aidl
package tk.munditv.mundichat;

// Declare any non-default types here with import statements

interface IChatServiceInterface {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    int getPid();
    void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat,
            double aDouble, String aString);
    void connect(String jsonAccount);
    void disconnect();
    void sendMessage(String remoteAccount, String message);
    void deleteMessage(long message_id);
    void sendPendingMessage();
    void login(String jsonAccount);
    void register(String jsonAccount);
    void addContact(String remoteAccount, String alias);
    void removeContact(String remoteAccount);
    void renameContact(String remoteAccount, String alias);
    void refreshContact(String remoteAccount);
    void clearConversations(String remoteAccount);
    void setAvatar(String path);
    void setPresence(int mode, String str);
    String getServiceRosterEntries();
    boolean isConnected();
    boolean isAuthenticated();
    String getDatabaseName();
}
