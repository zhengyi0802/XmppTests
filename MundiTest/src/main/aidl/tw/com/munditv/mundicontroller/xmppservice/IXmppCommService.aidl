// IXmppCommService.aidl
package tw.com.munditv.mundicontroller.xmppservice;

// Declare any non-default types here with import statements
import tw.com.munditv.mundicontroller.xmppservice.XmppSerivceCallback;

interface IXmppCommService {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat,
            double aDouble, String aString);
    boolean login(String username, String password);
    boolean isAuthenticated();
    void addGroup(String groupName);
    void removeGroup(String groupName);
    boolean addUser(String userName, String name, String groupName);
    boolean removeUser(String userName);
    void changeStateMessage(String status);
    boolean deleteAccount();
    boolean changePassword(String pwd);
    void registerCallback(XmppSerivceCallback callback);
}
