package tw.com.munditv.mundicontroller.xmppservice;

import android.util.Log;

import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.XMPPConnection;

public class XMConnectionListener implements ConnectionListener {

    private final static String TAG = "XMConnectionListener";

    public XMConnectionListener(String username, String password) {
        Log.d(TAG, "constructor");
    }

    @Override
    public void connected(XMPPConnection connection) {
        Log.d(TAG, "connected()");
    }

    @Override
    public void authenticated(XMPPConnection connection, boolean resumed) {
        Log.d(TAG, "authenticated() resumed = " + resumed );
    }

    @Override
    public void connectionClosed() {
        Log.d(TAG, "connectionClosed()");
    }

    @Override
    public void connectionClosedOnError(Exception e) {
        Log.d(TAG, "connectionClosedOnError()");
        e.printStackTrace();
    }
}
