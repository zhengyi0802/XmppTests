package tk.munditv.xmpp;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import org.jxmpp.stringprep.XmppStringprepException;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import tk.munditv.xmpp.database.SqLiteDatabase;
import tk.munditv.xmpp.database.models.Message;
import tk.munditv.xmpp.database.providers.MessagesProvider;

import static tk.munditv.xmpp.XmppAccount.PRESENCE_MODE_AVAILABLE;
import static tk.munditv.xmpp.XmppAccount.PRESENCE_MODE_CHAT;

public class MainActivity extends AppCompatActivity implements MessageCallback{

    private final static String TAG = "MainActivity";
    public static final String DATABASE_NAME = "messages.db";

    private XmppAccount mAccount;
    private XmppServiceBroadcastEventReceiver receiver;
    private MessagesProvider messagesProvider;
    private SqLiteDatabase mDatabase;
    private Context mContext;

    private ImageView   mQRCodeImage;
    private TextView    mSerialno;
    private TextView    mMessage;
    private TextView    mLoginStatus;
    private String      mSerialNumber;
    private String      mHostName;
    private String      mServiceName;
    private String      mResourceName;
    private String      mUsername;
    private String      mPassword;
    private String      mProductModel;
    private String      mQRString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        setContentView(R.layout.activity_main);
        Logger.info(TAG, "onCreate() start!");
        mMessage = findViewById(R.id.message_box);
        mSerialno = findViewById(R.id.label_serialno);
        mLoginStatus = findViewById(R.id.login_status);
        mQRCodeImage = findViewById(R.id.qrcode_image);
        mHostName = getString(R.string.string_hostname);
        mServiceName = getString(R.string.string_servicename);
        mResourceName = getString(R.string.string_resourcename);
        initialize();
    }

    private void initialize() {
        getSerialNumber();
        if(mProductModel.equals("Emulator")) {
            mUsername = "sdk1-" + mSerialNumber.toLowerCase();
        } else {
            mUsername = mProductModel.concat(" ") + "-" + mSerialNumber;
        }
        mUsername = mUsername.toLowerCase();
        mAccount = new XmppAccount();
        mQRString = "{\"host\":\"" + mHostName + "\",";
        mQRString = mQRString + "\"serialno\":\"" + mSerialNumber + "\",";
        mQRString = mQRString + "\"username\":\"" + mUsername + "\"}";
        if(mProductModel.equals("Emulator")) {
            mPassword = mSerialNumber.toLowerCase().substring(8);
        } else {
            mPassword = mSerialNumber.toLowerCase().substring(4);
        }
        Logger.debug(TAG, "QR String = " + mQRString);
        mAccount.setHost(mHostName);
        mAccount.setPort(443);
        mAccount.setXmppJid(mUsername);
        mAccount.setPassword(mPassword);
        mAccount.setServiceName(mServiceName);
        mAccount.setResourceName(mResourceName);
        mAccount.setPresenceMode(PRESENCE_MODE_AVAILABLE);
        Logger.debug(TAG, mAccount.toString());
        XmppServiceBroadcastEventEmitter.initialize(this, "tk.munditv.xmpp");
        receiver = new XmppServiceBroadcastEventReceiver();
        receiver.register(this);
        receiver.setMessageCallback(this);
        XmppServiceCommand.connect(this, mAccount);
        mDatabase = new SqLiteDatabase(this, DATABASE_NAME);
        messagesProvider = new MessagesProvider(mDatabase);
        QRCodeGenerator();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Logger.info(TAG, "onStart() start!");
    }

    @Override
    protected void onStop() {
        Logger.info(TAG, "onStop() start!");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Logger.info(TAG, "onDestroy() start!");
        super.onDestroy();
        XmppServiceCommand.disconnect(this);
        receiver.unregister(this);
        receiver = null;
    }

    @Override
    public void onMessageAdded(String remoteAccount, boolean incoming) throws XmppStringprepException {
        List<Message> arrayList = new ArrayList<Message>();
        arrayList = messagesProvider.getMessagesWithRecipient(mAccount.getXmppJid().toString()
                , remoteAccount);
        String message = "";
        for (Message m : arrayList) {
            message = m.getMessage();
            long timestamp = m.getCreationTimestamp();
            //String date = getDate(timestamp);
        }
        mMessage.setText(message);
        XmppServiceCommand.deleteMessage(this, 0);
    }

    @Override
    public void onConnected() {
        Logger.debug(TAG, "Connected!");
        mLoginStatus.setText(getString(R.string.str_connected));
    }

    @Override
    public void onDisconnected() {
        Logger.debug(TAG, "DisConnected!");
        mLoginStatus.setText(getString(R.string.str_disconnected));
        XmppServiceCommand.login(this, mAccount);
    }

    @Override
    public void onAuthenticated() {
        Logger.debug(TAG, "Authenticated!");
        mLoginStatus.setText(getString(R.string.str_login));
        XmppServiceCommand.setPresence(this, PRESENCE_MODE_AVAILABLE, "Ready to Chat");
    }

    @Override
    public void onAuthenticateFailure() {
        Logger.debug(TAG, "AuthenticateFailure!");
        mLoginStatus.setText(getString(R.string.str_tryregister));
        XmppServiceCommand.register(this, mAccount);
    }

    @Override
    public void onRegistered() {
        Logger.debug(TAG, "onRegistered!");
        mLoginStatus.setText(getString(R.string.str_registered));
        XmppServiceCommand.connect(this, mAccount);
    }

    @Override
    public void onRegisterFailure() {
        Logger.debug(TAG, "onRegisterFailure!");
        mLoginStatus.setText(getString(R.string.str_registerfailure));
        XmppServiceCommand.disconnect(this);
    }

    private void getSerialNumber() {
        try {
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("get", String.class);
            mSerialNumber = (String) get.invoke(c, "ro.serialno");
            mProductModel = (String) get.invoke(c, "ro.product.model");
            if( mProductModel.contains("Android SDK")) {
                mProductModel = "Emulator";
            }
            String str = getString(R.string.label_productmodel) + mProductModel + ", ";
            str += getString(R.string.label_serialno) + mSerialNumber;
            mSerialno.setText(str);
            Logger.debug(TAG , "serialnumber = " + mSerialNumber);
            Logger.debug(TAG , "product model = " + mProductModel);
        } catch (Exception ignored) {
            ignored.printStackTrace();
        }
        return;
    }

    private void QRCodeGenerator() {
        mHostName = getString(R.string.string_hostname);
        BarcodeEncoder encoder = new BarcodeEncoder();
        try{
            Bitmap bit = encoder.encodeBitmap(mQRString,
                    BarcodeFormat.QR_CODE,500,500);
            mQRCodeImage.setImageBitmap(bit);
        }catch (WriterException e){
            e.printStackTrace();
        }
    }

}