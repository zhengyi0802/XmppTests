package tk.munditv.xmpp;

import org.jxmpp.stringprep.XmppStringprepException;

public interface MessageCallback {
    public void onMessageAdded(String remoteAccount, boolean incoming) throws XmppStringprepException;
    public void onConnected();
    public void onDisconnected();
    public void onAuthenticated();
    public void onAuthenticateFailure();
    public void onRegistered();
    public void onRegisterFailure();
}
