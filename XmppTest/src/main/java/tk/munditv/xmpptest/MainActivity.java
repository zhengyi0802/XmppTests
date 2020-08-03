package tk.munditv.xmpptest;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import tk.munditv.xmppservice.Logger;
import tk.munditv.xmppservice.XmppAccount;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static String host = "webrtc01.mundi-tv.tk";
    private static String servicename = host;
    private static String resourceName = "xmpptest";
    private static String username = "chevylin";
    private static String password = "123456";

    private XmppAccount mXmppAccount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Logger.debug(TAG, "onCreate()");
        setContentView(R.layout.activity_main);
        initAccount();
    }

    private void initAccount() {
        Logger.debug(TAG, "initAccount()");
        mXmppAccount = new XmppAccount();
        mXmppAccount.setHost(host);
        mXmppAccount.setPort(443);
        mXmppAccount.setPresenceMode(XmppAccount.PRESENCE_MODE_AVAILABLE);
        mXmppAccount.setResourceName(resourceName);
        mXmppAccount.setPriority(0);
        mXmppAccount.setServiceName(servicename);
        mXmppAccount.setXmppJid(username);
        mXmppAccount.setPassword(password);
        MainApp.getInstance().connect(mXmppAccount);
    }

}
